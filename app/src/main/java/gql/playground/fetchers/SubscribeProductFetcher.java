package gql.playground.fetchers;

import org.dataloader.DataLoader;
import org.reactivestreams.Publisher;

import gql.playground.Loaders;
import gql.playground.enums.ProductType;
import gql.playground.models.Product;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;

public class SubscribeProductFetcher implements DataFetcher<Publisher<Product>> {
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
}
