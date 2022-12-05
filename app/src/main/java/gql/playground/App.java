package gql.playground;

import org.reactivestreams.Publisher;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

public class App {
    final Graph graph;
    final Loaders loaders;

    public App() {
        this.graph = new Graph(
            new Schema(
                new Runtime().getWiring()
            ).getSchema()
        );
        this.loaders = new Loaders();
    }

    public ExecutionResult query(String query) {
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
            .query(query)
            .dataLoaderRegistry(loaders.getRegistry())
            .build();

        return graph.query(executionInput);
    }

    public Publisher<ExecutionResult> subscribe(String query) {
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
            .query(query)
            .dataLoaderRegistry(loaders.getRegistry())
            .build();

        return graph.subscription(executionInput);
    }
}
