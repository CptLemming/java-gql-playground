package gql.playground;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;

import akka.Done;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.Receptionist;
import gql.playground.actors.ProductActor;
import graphql.ExecutionInput;
import graphql.ExecutionResult;

public class App {
    final Graph graph;
    final Loaders loaders;
    final ActorSystem<Void> system;
    CompletableFuture<Done> isReady = new CompletableFuture<>();
    ActorRef<ProductActor.Command> productRef;

    public App() {
        this.graph = new Graph(
            new Schema(
                new Runtime().getWiring()
            ).getSchema()
        );
        this.loaders = new Loaders();
        this.system = ActorSystem.create(
            Behaviors.setup(context -> {
                System.out.println("CREATE ACTOR SYSTEM");
                context.spawn(ProductActor.create(), "product-manager");
                isReady.complete(Done.getInstance());
                return Behaviors.empty();
              }),
              "root"
        );
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

    public synchronized CompletionStage<ActorRef<ProductActor.Command>> getProductActor() {
        if (Optional.ofNullable(productRef).isEmpty()) {
            System.out.println("LOOKUP PRODUCT ACTOR");
            return lookupProductActor();
        }
        System.out.println("CACHED PRODUCT ACTOR");
        return CompletableFuture.completedStage(productRef);
    }

    private CompletionStage<ActorRef<ProductActor.Command>> lookupProductActor() {
        return AskPattern
          .<Receptionist.Command, Receptionist.Listing>ask(
            system.receptionist(),
            replyTo -> Receptionist.find(ProductActor.SERVICE_KEY, replyTo),
            Duration.ofSeconds(5),
            system.scheduler()
          )
          .thenApply(listing -> {
            AtomicReference<ActorRef<ProductActor.Command>> result = new AtomicReference<>(null);
            listing.getServiceInstances(ProductActor.SERVICE_KEY).stream().findFirst().ifPresent(result::set);
            return Optional.ofNullable(result.get());
          })
          .thenCompose(result -> {
            if (!result.isPresent()) return CompletableFuture.failedStage(new Exception("Cannot find actor"));

            this.productRef = result.get();
            return CompletableFuture.completedStage(result.get());
          });
    }
}
