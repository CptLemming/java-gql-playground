package gql.playground;

import org.reactivestreams.Publisher;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.SubscriptionExecutionStrategy;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions;
import graphql.schema.GraphQLSchema;

public class Graph {
  final GraphQL graph;

  public Graph(GraphQLSchema schema) {
    this.graph = build(schema);
  }

  private GraphQL build(GraphQLSchema graphQLSchema) {
    DataLoaderDispatcherInstrumentationOptions options = DataLoaderDispatcherInstrumentationOptions
      .newOptions()
      .includeStatistics(true);
    DataLoaderDispatcherInstrumentation dispatcherInstrumentation = new DataLoaderDispatcherInstrumentation(options);

    return GraphQL.newGraphQL(graphQLSchema)
      .instrumentation(dispatcherInstrumentation)
      .queryExecutionStrategy(new AsyncExecutionStrategy())
      .subscriptionExecutionStrategy(new SubscriptionExecutionStrategy())
      .build();
  }

  public ExecutionResult query(ExecutionInput executionInput) {
    return graph.execute(executionInput);
  }

  public Publisher<ExecutionResult> subscription(ExecutionInput executionInput) {
    ExecutionResult result = graph.execute(executionInput);

    if (result.getErrors().size() > 0) {
      System.err.println(result.getErrors());
      throw new RuntimeException("SUB FAILED");
    }

    return result.getData();
  }

  public GraphQL getGraph() {
    return graph;
  }
}
