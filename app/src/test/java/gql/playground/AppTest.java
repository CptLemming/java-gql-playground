package gql.playground;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import graphql.ExecutionResult;
import io.reactivex.rxjava3.core.Observable;

class AppTest {
    @Test void helloWorld() {
        App app = new App();

        ExecutionResult executionResult = app.query("query { hello }");

        System.out.println("== DATA");
        System.out.println((Object) executionResult.getData());
        System.out.println("** ERR");
        System.err.println(executionResult.getErrors());
    }

    @Test void getFader() {
        App app = new App();

        ExecutionResult executionResult = app.query("query GET_FADER { fader { id, label, isAccessed }}");

        System.out.println("== DATA");
        System.out.println((Object) executionResult.getData());
        System.out.println("** ERR");
        System.err.println(executionResult.getErrors());
    }

    @Test void getFaders() {
        App app = new App();

        ExecutionResult executionResult = app.query("query GET_FADERS { faders { id, label, isAccessed, type }}");

        System.out.println("== DATA");
        System.out.println((Object) executionResult.getData());
        System.out.println("** ERR");
        System.err.println(executionResult.getErrors());
    }

    @Test void getAliasFader() {
        App app = new App();

        ExecutionResult executionResult = app.query("query GET_FADER { faderA: fader(faderID: \"L1F1\") { id, label, isAccessed, type } faderB: fader(faderID: \"L1F2\") { id, label, isAccessed, type }}");

        System.out.println("== DATA");
        System.out.println((Object) executionResult.getData());
        System.out.println("** ERR");
        System.err.println(executionResult.getErrors());
    }

    @Test void subscribeFader() {
        App app = new App();

        Publisher<ExecutionResult> dataStream = app.subscribe("subscription GET_FADER { fader { id, label, isAccessed, type }}");

        Observable.fromPublisher(dataStream).blockingSubscribe(executionResult -> {
            System.out.println("== DATA");
            System.out.println((Object) executionResult.getData());
            System.out.println("** ERR");
            System.err.println(executionResult.getErrors());
        });
    }

    @Test void retrieveFaderActor() throws InterruptedException, ExecutionException {
        App app = new App();
        Context ctx = new Context(app.getSystem());
        System.out.println(">> Starting");

        for (int i = 0; i < 10; i++) {
            System.out.println("BEGIN FETCH : "+ i);
            long runStart = System.nanoTime();
            ctx.getFaderActor().toCompletableFuture().join();
            long runEnd = System.nanoTime();
            System.out.println("END FETCH : "+ Duration.ofNanos(runEnd - runStart).toNanos() + "ns");
        }

        System.out.println(">> End");
    }
}
