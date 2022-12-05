package gql.playground.fetchers;

import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;

import gql.playground.Loaders;
import gql.playground.enums.ProductType;
import gql.playground.models.Product;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.Observable;

public class QueryProductFetcher implements DataFetcher<CompletableFuture<Product>>{
    @Override
    public CompletableFuture<Product> get(DataFetchingEnvironment environment) {
        DataLoader<ProductType, Observable<Product>> dataLoader = Loaders.getProductsLoader(environment);

        return dataLoader.load(ProductType.SOCKS).thenCompose(obs -> obs.firstOrErrorStage());
    }
}
