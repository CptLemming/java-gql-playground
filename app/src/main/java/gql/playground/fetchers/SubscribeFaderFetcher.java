package gql.playground.fetchers;

import java.util.Optional;

import org.dataloader.DataLoader;
import org.reactivestreams.Publisher;

import gql.playground.Loaders;
import gql.playground.enums.PathType;
import gql.playground.models.Fader;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;

public class SubscribeFaderFetcher implements DataFetcher<Publisher<Fader>> {
    @Override
    public Publisher<Fader> get(DataFetchingEnvironment environment) {
        DataLoader<PathType, Observable<Fader>> dataLoader = Loaders.getFadersLoader(environment);
        PathType pathType = Optional.<String>ofNullable(environment.getArgument("pathType"))
            .map(PathType::valueOf)
            .orElse(PathType.CHANNEL);

        return Observable.fromCompletionStage(dataLoader.load(pathType))
            .flatMap(x -> x)
            .doOnNext(msg -> {
                System.out.println("RECV -> MSG");
            })
            .take(1)
            .toFlowable(BackpressureStrategy.LATEST);
    }
}
