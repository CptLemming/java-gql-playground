package gql.playground;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.util.function.BiFunction;
import java.time.Duration;

public class ActorApp {
  public static Integer TICKER_MS = 10;
  public static Integer NUM_CHILDREN = 1000;
  public static void main(String[] args) {
    BiFunction<Integer, Subject<String>, Behavior<ChildCommand>> createChild = (index, parentSubject) -> Behaviors.withTimers(timers -> Behaviors.setup(context -> {
      Subject<String> subject = PublishSubject.<String>create();
      Observable.interval(ActorApp.TICKER_MS, TimeUnit.MILLISECONDS)
        .subscribe(ticker -> {
          context.getSelf().tell(new SendMessageToChild("PING"));
        });

      subject
        // .observeOn(Schedulers.single(), false, 16)
        .subscribe(msg -> parentSubject.onNext(msg));

      timers.startTimerAtFixedRate("ticker", new ChildHealthCheck(), Duration.ofSeconds(1));

      return Behaviors.receive(ChildCommand.class)
        .onMessage(SendMessageToChild.class, command -> {
          subject.onNext(String.format("Child %d sends %s", index, command.message));
          return Behaviors.same();
        })
        .onMessage(ChildHealthCheck.class, command -> {
          System.out.println(String.format("Child %d is OK", index));
          return Behaviors.same();
        })
        .build();
    }));

    Behavior<ParentCommand> parent = Behaviors.setup(context -> {
      Subject<String> rootSubject = PublishSubject.<String>create();
      rootSubject
        // .subscribeOn(Schedulers.single())
        // .observeOn(Schedulers.single())
        // .unsubscribeOn(Schedulers.single())
        .subscribe(msg -> {
          System.out.println("Parent received -> "+ msg);
        });

      for (int i = 0; i < ActorApp.NUM_CHILDREN; i++) {
        context.getSelf().tell(new CreateChild(i));
      }

      return Behaviors.receive(ParentCommand.class)
        .onMessage(CreateChild.class, (command) -> {
          context.spawn(createChild.apply(command.index, rootSubject), String.format("child-%d", command.index));
          return Behaviors.same();
        })
        .build();
    });

    Behavior<Done> root = Behaviors.setup(context -> {
      System.out.println("CREATE ACTOR SYSTEM");
      context.spawn(parent, "parent");
      return Behaviors.empty();
    });

    ActorSystem.create(root, "root");
  }

  public static interface ParentCommand {}
  public static record CreateChild(Integer index) implements ParentCommand {}

  public static interface ChildCommand {}
  public static record SendMessageToChild(String message) implements ChildCommand {}
  public static record ChildHealthCheck() implements ChildCommand {}
}
