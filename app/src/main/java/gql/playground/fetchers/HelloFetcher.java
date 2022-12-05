package gql.playground.fetchers;

import java.util.concurrent.CompletableFuture;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class HelloFetcher implements DataFetcher<CompletableFuture<String>> {
  @Override
  public CompletableFuture<String> get(DataFetchingEnvironment environment) {
      return CompletableFuture.completedFuture("world!");
  }
}
