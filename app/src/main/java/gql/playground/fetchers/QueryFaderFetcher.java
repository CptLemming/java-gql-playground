package gql.playground.fetchers;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;

import gql.playground.Loaders;
import gql.playground.models.Fader;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.Observable;

public class QueryFaderFetcher implements DataFetcher<CompletableFuture<Fader>>{
    @Override
    public CompletableFuture<Fader> get(DataFetchingEnvironment environment) {
        DataLoader<String, Observable<Fader>> dataLoader = Loaders.getFadersLoader(environment);
        String faderID = Optional.<String>ofNullable(environment.getArgument("faderID"))
            .orElse("L1F1");

        return dataLoader.load(faderID).thenCompose(obs -> obs.firstOrErrorStage());
    }
}
