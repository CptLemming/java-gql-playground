package gql.playground.fetchers;

import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;

import gql.playground.Loaders;
import gql.playground.models.Fader;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class FaderIsAccessedFetcher implements DataFetcher<CompletableFuture<Boolean>> {

    @Override
    public CompletableFuture<Boolean> get(DataFetchingEnvironment environment) {
        Fader fader = environment.getSource();
        DataLoader<String, Boolean> dataLoader = Loaders.getAccessedLoader(environment);

        return dataLoader.load(fader.getId(), fader);
    }
}
