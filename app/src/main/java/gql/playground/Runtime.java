package gql.playground;

import gql.playground.fetchers.*;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;

public class Runtime {
  final RuntimeWiring runtimeWiring;

  public Runtime() {
    this.runtimeWiring = build();
  }

  private RuntimeWiring build() {
    return RuntimeWiring.newRuntimeWiring()
      .type("Query", builder -> builder
          .dataFetcher("hello", new HelloFetcher())
          .dataFetcher("products", new QueryProductsFetcher())
          .dataFetcher("product", new QueryProductFetcher())
      )
      .type("Product", builder -> builder
          .dataFetcher("cost", new StaticDataFetcher(Float.valueOf("1.00")))
          .dataFetcher("tax", new ProductTaxFetcher())
      )
      .type("Subscription", builder -> builder
          .dataFetcher("product", new SubscribeProductFetcher())
      )
      .build();
  }

  public RuntimeWiring getWiring() {
    return runtimeWiring;
  }
}
