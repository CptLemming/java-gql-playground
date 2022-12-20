package gql.playground.fetchers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;

import gql.playground.Loaders;
import gql.playground.models.Fader;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.Observable;

public class QueryFadersFetcher implements DataFetcher<CompletableFuture<List<Fader>>>{

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<Fader>> get(DataFetchingEnvironment environment) {
        DataLoader<String, Observable<Fader>> dataLoader = Loaders.getFadersLoader(environment);

        return dataLoader.loadMany(Arrays.asList("L1F1", "L1F2"))
            .thenCompose(obs -> {
                return Observable.combineLatestArray(obs.stream().toArray(Observable[]::new), (entries) -> entries)
                .firstOrErrorStage();
            });
    }
}
