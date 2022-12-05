package gql.playground.loaders;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.dataloader.BatchLoader;

import gql.playground.enums.ProductType;
import gql.playground.models.Product;
import io.reactivex.rxjava3.core.Observable;

public class ProductLoader implements BatchLoader<ProductType, Observable<Product>> {

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
