package gql.playground;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.dataloader.BatchLoader;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.BatchLoaderWithContext;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderRegistry;

import gql.playground.enums.ProductType;
import gql.playground.models.Product;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.Observable;

public class Loaders {
  final DataLoaderRegistry registry;

  public Loaders() {
    this.registry = build();
  }

  private DataLoaderRegistry build() {
    BatchLoaderWithContext<String, Float> taxBatchLoader = new BatchLoaderWithContext<String, Float>() {
      @Override
      public CompletionStage<List<Float>> load(List<String> keys, BatchLoaderEnvironment loaderContext) {
          List<Float> data = new ArrayList<>();
          System.out.println("TAX Loader :: "+ keys);
          Map<Object, Object> keysContext = loaderContext.getKeyContexts();

          for (int i = 0; i < keys.size(); i++) {
              Product product = (Product) keysContext.get(keys.get(i));
              data.add(Float.valueOf("20.50") * Integer.parseInt(product.getId()));
          }

          return CompletableFuture.completedStage(data);
      }
  };

  BatchLoader<ProductType, Observable<Product>> productsBatchLoader = new BatchLoader<ProductType, Observable<Product>>() {
      @Override
      public CompletionStage<List<Observable<Product>>> load(List<ProductType> keys) {
          List<Product> data = new ArrayList<>();
          System.out.println("PRODUCTS Loader :: "+ keys);

          for (int i = 1; i <= keys.size(); i++) {
              String ID = Integer.toString(i);
              String name = String.format("P%d", i);
              data.add(new Product(ID, name, keys.get(i - 1)));
          }

          return CompletableFuture.supplyAsync(() -> data.stream().map(Observable::just).toList());
      }
  };

    DataLoader<ProductType, Observable<Product>> productsDataLoader = DataLoaderFactory.newDataLoader(productsBatchLoader);
    DataLoader<String, Float> taxDataLoader = DataLoaderFactory.newDataLoader(taxBatchLoader);
    DataLoaderRegistry registry = new DataLoaderRegistry();
    registry.register(DataLoaders.PRODUCTS.name(), productsDataLoader);
    registry.register(DataLoaders.TAX.name(), taxDataLoader);

    return registry;
  }

  public DataLoaderRegistry getRegistry() {
    return registry;
  }

  public static enum DataLoaders {
      PRODUCTS,
      TAX
  }

  public static DataLoader<ProductType, Observable<Product>> getProductsLoader(DataFetchingEnvironment environment) {
      return environment.getDataLoader(DataLoaders.PRODUCTS.name());
  }

  public static DataLoader<String, Float> getTaxLoader(DataFetchingEnvironment environment) {
      return environment.getDataLoader(DataLoaders.TAX.name());
  }
}
