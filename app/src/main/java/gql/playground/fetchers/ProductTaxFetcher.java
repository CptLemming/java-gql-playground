package gql.playground.fetchers;

import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;

import gql.playground.Loaders;
import gql.playground.models.Product;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class ProductTaxFetcher implements DataFetcher<CompletableFuture<Float>> {

    @Override
    public CompletableFuture<Float> get(DataFetchingEnvironment environment) {
        Product product = environment.getSource();
        DataLoader<String, Float> dataLoader = Loaders.getTaxLoader(environment);

        return dataLoader.load(product.getId(), product);
    }
}
