package gql.playground;

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

    @Test void getProduct() {
        App app = new App();

        ExecutionResult executionResult = app.query("query GET_PRODUCT { product { id, name, cost, tax }}");

        System.out.println("== DATA");
        System.out.println((Object) executionResult.getData());
        System.out.println("** ERR");
        System.err.println(executionResult.getErrors());
    }

    @Test void getProducts() {
        App app = new App();

        ExecutionResult executionResult = app.query("query GET_PRODUCTS { products { id, name, cost, tax, type }}");

        System.out.println("== DATA");
        System.out.println((Object) executionResult.getData());
        System.out.println("** ERR");
        System.err.println(executionResult.getErrors());
    }

    @Test void getAliasProduct() {
        App app = new App();

        ExecutionResult executionResult = app.query("query GET_PRODUCT { productA: product(productType:SOCKS) { id, name, cost, tax, type } productB: product(productType:PANTS) { id, name, cost, tax, type }}");

        System.out.println("== DATA");
        System.out.println((Object) executionResult.getData());
        System.out.println("** ERR");
        System.err.println(executionResult.getErrors());
    }

    @Test void subscribeProduct() {
        App app = new App();

        Publisher<ExecutionResult> dataStream = app.subscribe("subscription GET_PRODUCT { product { id, name, cost, tax, type }}");

        Observable.fromPublisher(dataStream).blockingSubscribe(executionResult -> {
            System.out.println("== DATA");
            System.out.println((Object) executionResult.getData());
            System.out.println("** ERR");
            System.err.println(executionResult.getErrors());
        });
    }
}
