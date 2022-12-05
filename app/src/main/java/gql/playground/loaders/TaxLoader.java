package gql.playground.loaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.BatchLoaderWithContext;

import gql.playground.models.Product;

public class TaxLoader implements BatchLoaderWithContext<String, Float> {
  
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
}