package gql.playground.fetchers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;

import gql.playground.Loaders;
import gql.playground.enums.ProductType;
import gql.playground.models.Product;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.Observable;

public class QueryProductsFetcher implements DataFetcher<CompletableFuture<List<Product>>>{

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<Product>> get(DataFetchingEnvironment environment) {
        DataLoader<ProductType, Observable<Product>> dataLoader = Loaders.getProductsLoader(environment);

        return dataLoader.loadMany(Arrays.asList(ProductType.SOCKS, ProductType.PANTS))
            .thenCompose(obs -> {
                return Observable.combineLatestArray(obs.stream().toArray(Observable[]::new), (entries) -> entries)
                .firstOrErrorStage();
            });
    }
}
