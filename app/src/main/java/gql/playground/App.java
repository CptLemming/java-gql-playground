package gql.playground;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.persistence.query.EventEnvelope;
import akka.persistence.query.PersistenceQuery;
import akka.stream.javadsl.Source;
import gql.playground.actors.FaderActor;
import gql.playground.actors.MyJavadslReadJournal;
import graphql.ExecutionInput;
import graphql.ExecutionResult;

public class App {
    final Graph graph;
    final Loaders loaders;
    final ActorSystem<Void> system;
    CompletableFuture<Done> isReady = new CompletableFuture<>();

    public static void main(String[] args) {
        App app = new App();
        // String query = "query GET_FADERS { faders { id, label, isAccessed, type }}";
        // System.out.println("Running query :: "+ query);

        // ExecutionResult executionResult = app.query(query);

        // System.out.println("== DATA");
        // System.out.println((Object) executionResult.getData());
        // System.out.println("** ERR");
        // System.err.println(executionResult.getErrors());

        // Config customConf = ConfigFactory.load("custom.conf"); // .parseString("akka.log-config-on-start = on");

        // final MyJavadslReadJournal readJournal =
        //     PersistenceQuery.get(app.system)
        //         .getReadJournalFor(
        //             MyJavadslReadJournal.class, "akka.persistence.query.my-journal", customConf);

        // // issue query to journal
        // Source<EventEnvelope, NotUsed> source =
        //     readJournal.eventsByPersistenceId("user-1337", 0, Long.MAX_VALUE);

        // // materialize stream, consuming events
        // source.runForeach(event -> System.out.println("Event: " + event), app.system);
    }

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
