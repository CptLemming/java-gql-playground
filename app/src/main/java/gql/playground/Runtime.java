package gql.playground;

import gql.playground.fetchers.*;
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
          .dataFetcher("faders", new QueryFadersFetcher())
          .dataFetcher("fader", new QueryFaderFetcher())
      )
      .type("Fader", builder -> builder
          .dataFetcher("isAccessed", new FaderIsAccessedFetcher())
      )
      .type("Subscription", builder -> builder
          .dataFetcher("fader", new SubscribeFaderFetcher())
      )
      .build();
  }

  public RuntimeWiring getWiring() {
    return runtimeWiring;
  }
}
