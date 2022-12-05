package gql.playground.fetchers;

import java.util.Optional;
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
        ProductType productType = Optional.<String>ofNullable(environment.getArgument("productType"))
            .map(ProductType::valueOf)
            .orElse(ProductType.SOCKS);

        return dataLoader.load(productType).thenCompose(obs -> obs.firstOrErrorStage());
    }
}
