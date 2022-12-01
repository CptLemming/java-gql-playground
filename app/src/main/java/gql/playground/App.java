package gql.playground;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderRegistry;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
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

public class App {
    public static void main(String[] args) {
        String schema = "type Query { \n";
        schema += "  hello: String \n";
        schema += "  products: [Product] \n";
        schema += "} \n";
        schema += "type Product { \n";
        schema += "  id: ID \n";
        schema += "  name: String \n";
        schema += "  description: String \n";
        schema += "  cost: Float \n";
        schema += "  tax: Float \n";
        schema += "} \n";

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        BatchLoader<String, Object> productsBatchLoader = new BatchLoader<String, Object>() {
            @Override
            public CompletionStage<List<Object>> load(List<String> keys) {
                Map<String, Map<String, Object>> data = new HashMap<>();
                data.put("1", new HashMap<>(){{
                    put("id", "1");
                    put("name", "P1");
                }});
                data.put("2", new HashMap<>(){{
                    put("id", "2");
                    put("name", "P2");
                }});

                return CompletableFuture.completedFuture((List)data.values().stream().toList());
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
                .dataFetcher("products", new DataFetcher<CompletableFuture<List<Object>>>() {
                    @Override
                    public CompletableFuture<List<Object>> get(DataFetchingEnvironment environment) {
                        DataLoader<String, Object> dataLoader = environment.getDataLoader("products");
                        return dataLoader.loadMany(Arrays.asList("1", "2"));
                    }
                })
            ).build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        DataLoaderDispatcherInstrumentationOptions options = DataLoaderDispatcherInstrumentationOptions
            .newOptions()
            .includeStatistics(true);
        DataLoaderDispatcherInstrumentation dispatcherInstrumentation = new DataLoaderDispatcherInstrumentation(options);

        GraphQL build = GraphQL.newGraphQL(graphQLSchema)
            .instrumentation(dispatcherInstrumentation)
            // .queryExecutionStrategy(new AsyncExecutionStrategy())
            // .subscriptionExecutionStrategy(new AsyncExecutionStrategy())
            .build();

        DataLoader<String, Object> productsDataLoader = DataLoaderFactory.newDataLoader(productsBatchLoader);
        DataLoaderRegistry registry = new DataLoaderRegistry();
        registry.register("products", productsDataLoader);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
            .query("{ products { id, name, cost, tax }}")
            .dataLoaderRegistry(registry)
            .build();

        // ExecutionResult executionResult = build.execute("{hello}");
        ExecutionResult executionResult = build.execute(executionInput);

        System.out.println("== DATA");
        System.out.println(executionResult.getData().toString());
        System.out.println("** ERR");
        System.err.println(executionResult.getErrors());
        // Prints: {hello=world}
    }
}
