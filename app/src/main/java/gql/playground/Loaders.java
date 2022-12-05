package gql.playground;

import org.dataloader.BatchLoader;
import org.dataloader.BatchLoaderWithContext;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderRegistry;

import gql.playground.enums.ProductType;
import gql.playground.loaders.ProductLoader;
import gql.playground.loaders.TaxLoader;
import gql.playground.models.Product;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.Observable;

public class Loaders {
  final DataLoaderRegistry registry;

  public Loaders() {
    this.registry = build();
  }

  private DataLoaderRegistry build() {
    BatchLoaderWithContext<String, Float> taxBatchLoader = new TaxLoader();
    BatchLoader<ProductType, Observable<Product>> productsBatchLoader = new ProductLoader();

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
