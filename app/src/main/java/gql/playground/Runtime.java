package gql.playground;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.reactivestreams.Publisher;

import gql.playground.enums.ProductType;
import gql.playground.models.Product;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;

public class Runtime {
  final RuntimeWiring runtimeWiring;

  public Runtime() {
    this.runtimeWiring = build();
  }

  private RuntimeWiring build() {
    return RuntimeWiring.newRuntimeWiring()
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
                  DataLoader<ProductType, Observable<Product>> dataLoader = Loaders.getProductsLoader(environment);

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
                  DataLoader<ProductType, Observable<Product>> dataLoader = Loaders.getProductsLoader(environment);

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
                  DataLoader<String, Float> dataLoader = Loaders.getTaxLoader(environment);

                  return dataLoader.load(product.getId(), product);
              }
          })
      )
      .type("Subscription", builder -> builder
          .dataFetcher("product", new DataFetcher<Publisher<Product>>() {
              @Override
              public Publisher<Product> get(DataFetchingEnvironment environment) {
                  DataLoader<ProductType, Observable<Product>> dataLoader = Loaders.getProductsLoader(environment);

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
  }

  public static RuntimeWiring withRegistry() {
    return new Runtime().getWiring();
  }

  public RuntimeWiring getWiring() {
    return runtimeWiring;
  }
}
