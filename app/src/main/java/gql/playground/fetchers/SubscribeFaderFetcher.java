package gql.playground.fetchers;

import java.util.Optional;

import org.dataloader.DataLoader;
import org.reactivestreams.Publisher;

import gql.playground.Loaders;
import gql.playground.models.Fader;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;

public class SubscribeFaderFetcher implements DataFetcher<Publisher<Fader>> {
    @Override
    public Publisher<Fader> get(DataFetchingEnvironment environment) {
        DataLoader<String, Observable<Fader>> dataLoader = Loaders.getFadersLoader(environment);
        String faderID = Optional.<String>ofNullable(environment.getArgument("faderID"))
            .orElse("L1F1");

        return Observable.fromCompletionStage(dataLoader.load(faderID))
            .flatMap(x -> x)
            .doOnNext(msg -> {
                System.out.println("RECV -> MSG");
            })
            .take(1)
            .toFlowable(BackpressureStrategy.LATEST);
    }
}
