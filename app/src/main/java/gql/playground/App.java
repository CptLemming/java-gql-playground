package gql.playground;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderRegistry;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.SubscriptionExecutionStrategy;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;

public class App {
    public static void main(String[] args) {
        InputStream schema = App.class.getClassLoader().getResourceAsStream("schema.graphql");

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        BatchLoader<String, Float> taxBatchLoader = new BatchLoader<String, Float>() {
            @Override
            public CompletionStage<List<Float>> load(List<String> keys) {
                List<Float> data = new ArrayList<>();
                System.out.println("LOAD TAX :: "+ keys);

                for (int i = 0; i < keys.size(); i++) {
                    data.add(Float.valueOf("20.50"));
                }

                return CompletableFuture.completedStage(data);
            }
        };

        BatchLoader<ProductType, Observable<Product>> productsBatchLoader = new BatchLoader<ProductType, Observable<Product>>() {
            @Override
            public CompletionStage<List<Observable<Product>>> load(List<ProductType> keys) {
                List<Product> data = new ArrayList<>();

                for (int i = 1; i <= keys.size(); i++) {
                    String ID = Integer.toString(i);
                    String name = String.format("P%d", i);
                    data.add(new Product(ID, name, Float.valueOf(0), Float.valueOf(0), keys.get(i - 1)));
                }

                return CompletableFuture.supplyAsync(() -> data.stream().map(Observable::just).toList());
            }
        };

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", builder -> builder
                .dataFetcher("hello", new DataFetcher<CompletableFuture<String>>() {
                    @Override
                    public CompletableFuture<String> get(DataFetchingEnvironment environment) {
                        return CompletableFuture.completedFuture("world!");
                    }
                })
                .dataFetcher("products", new DataFetcher<CompletableFuture<List<Product>>>() {
                    @Override
                    public CompletableFuture<List<Product>> get(DataFetchingEnvironment environment) {
                        DataLoader<ProductType, Observable<Product>> dataLoader = environment.getDataLoader(DataLoaders.PRODUCTS.name());

                        return dataLoader.loadMany(Arrays.asList(ProductType.SOCKS, ProductType.PANTS))
                            .thenCompose(obs -> {
                                return Observable.combineLatestArray(obs.stream().toArray(Observable[]::new), (entries) -> entries)
                                .firstOrErrorStage();
                            });
                    }
                })
                .dataFetcher("product", new DataFetcher<CompletableFuture<Product>>() {
                    @Override
                    public CompletableFuture<Product> get(DataFetchingEnvironment environment) {
                        DataLoader<ProductType, Observable<Product>> dataLoader = environment.getDataLoader(DataLoaders.PRODUCTS.name());

                        return dataLoader.load(ProductType.SOCKS).thenCompose(obs -> obs.firstOrErrorStage());
                    }
                })
            )
            .type("Product", builder -> builder
                .dataFetcher("cost", new StaticDataFetcher(Float.valueOf("1.00")))
                .dataFetcher("tax", new DataFetcher<CompletableFuture<Float>>() {
                    @Override
                    public CompletableFuture<Float> get(DataFetchingEnvironment environment) {
                        Product product = environment.getSource();
                        DataLoader<String, Float> dataLoader = environment.getDataLoader(DataLoaders.TAX.name());

                        return dataLoader.load(product.id);
                    }
                })
            )
            .type("Subscription", builder -> builder
                .dataFetcher("product", new DataFetcher<Publisher<Product>>() {
                    @Override
                    public Publisher<Product> get(DataFetchingEnvironment environment) {
                        DataLoader<ProductType, Observable<Product>> dataLoader = environment.getDataLoader(DataLoaders.PRODUCTS.name());

                        return Observable.fromCompletionStage(dataLoader.load(ProductType.SOCKS))
                            .flatMap(x -> x)
                            .doOnNext(msg -> {
                                System.out.println("RECV -> MSG");
                            })
                            .take(1)
                            .toFlowable(BackpressureStrategy.LATEST);
                    }
                })
            )
            .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        DataLoaderDispatcherInstrumentationOptions options = DataLoaderDispatcherInstrumentationOptions
            .newOptions()
            .includeStatistics(true);
        DataLoaderDispatcherInstrumentation dispatcherInstrumentation = new DataLoaderDispatcherInstrumentation(options);

        GraphQL build = GraphQL.newGraphQL(graphQLSchema)
            .instrumentation(dispatcherInstrumentation)
            .queryExecutionStrategy(new AsyncExecutionStrategy())
            .subscriptionExecutionStrategy(new SubscriptionExecutionStrategy())
            .build();

        DataLoader<ProductType, Observable<Product>> productsDataLoader = DataLoaderFactory.newDataLoader(productsBatchLoader);
        DataLoader<String, Float> taxDataLoader = DataLoaderFactory.newDataLoader(taxBatchLoader);
        DataLoaderRegistry registry = new DataLoaderRegistry();
        registry.register(DataLoaders.PRODUCTS.name(), productsDataLoader);
        registry.register(DataLoaders.TAX.name(), taxDataLoader);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
            // .query("subscription GET_PRODUCT { product { id, name, cost, tax }}")
            .query("query GET_PRODUCT { products { id, name, cost, tax, type }}")
            .dataLoaderRegistry(registry)
            .build();

        // ExecutionResult executionResult = build.execute("{hello}");
        ExecutionResult executionResult = build.execute(executionInput);

        System.out.println("== DATA");
        System.out.println((Object) executionResult.getData());
        System.out.println("** ERR");
        System.err.println(executionResult.getErrors());

        // Publisher<ExecutionResult> dataStream = executionResult.getData();

        // AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
        // dataStream.subscribe(new Subscriber<ExecutionResult>() {

        //     @Override
        //     public void onSubscribe(Subscription s) {
        //         System.out.println("DO SUBSCRIBE");
        //         subscriptionRef.set(s);
        //         s.request(1);
        //     }

        //     @Override
        //     public void onNext(ExecutionResult er) {
        //         System.out.println("GOT ON NEXT DATA");
        //         System.out.println((Object) er.getData());
        //         subscriptionRef.get().request(1);
        //     }

        //     @Override
        //     public void onError(Throwable t) {
        //         System.out.println("DO ERROR");
        //     }
        
        //     @Override
        //     public void onComplete() {
        //         System.out.println("DO COMPLETE");
        //     }
        // });
        // Prints: {hello=world}
    }

    public static class Product {
        final String id;
        final String name;
        final Float cost;
        final Float tax;
        final ProductType type;

        public Product(String id, String name, Float cost, Float tax, ProductType type) {
            this.id = id;
            this.name = name;
            this.cost = cost;
            this.tax = tax;
            this.type = type;
        }
    }

    public static enum ProductType {
        SOCKS,
        PANTS
    }

    public static enum DataLoaders {
        PRODUCTS,
        TAX
    }
}
