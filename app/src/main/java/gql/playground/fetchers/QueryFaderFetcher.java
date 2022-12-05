package gql.playground.fetchers;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;

import gql.playground.Loaders;
import gql.playground.enums.PathType;
import gql.playground.models.Fader;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.Observable;

public class QueryFaderFetcher implements DataFetcher<CompletableFuture<Fader>>{
    @Override
    public CompletableFuture<Fader> get(DataFetchingEnvironment environment) {
        DataLoader<PathType, Observable<Fader>> dataLoader = Loaders.getFadersLoader(environment);
        PathType pathType = Optional.<String>ofNullable(environment.getArgument("pathType"))
            .map(PathType::valueOf)
            .orElse(PathType.CHANNEL);

        return dataLoader.load(pathType).thenCompose(obs -> obs.firstOrErrorStage());
    }
}
