package gql.playground;

import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;

import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import gql.playground.actors.FaderActor;
import graphql.ExecutionInput;
import graphql.ExecutionResult;

public class App {
    final Graph graph;
    final Loaders loaders;
    final ActorSystem<Void> system;
    CompletableFuture<Done> isReady = new CompletableFuture<>();

    public App() {
        this.graph = new Graph(
            new Schema(
                new Runtime().getWiring()
            ).getSchema()
        );
        this.system = ActorSystem.create(
            Behaviors.setup(context -> {
                System.out.println("CREATE ACTOR SYSTEM");
                context.spawn(FaderActor.create(), "fader-manager");
                isReady.complete(Done.getInstance());
                return Behaviors.empty();
              }),
              "root"
        );
        this.loaders = new Loaders(system);
        while (!isReady.isDone()) {
            System.out.println("STALL...");
        }
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

    public ActorSystem<Void> getSystem() {
        return system;
    }
}
