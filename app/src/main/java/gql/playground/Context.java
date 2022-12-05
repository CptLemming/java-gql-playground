package gql.playground;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.receptionist.Receptionist;
import gql.playground.actors.FaderActor;

public class Context {
  final ActorSystem<Void> system;
  ActorRef<FaderActor.Command> faderRef;

  public Context(ActorSystem<Void> system) {
    this.system = system;
  }

  public ActorSystem<Void> getSystem() {
    return system;
  }

  public synchronized CompletionStage<ActorRef<FaderActor.Command>> getFaderActor() {
      if (Optional.ofNullable(faderRef).isEmpty()) {
          System.out.println("LOOKUP FADER ACTOR");
          return lookupFaderActor();
      }
      System.out.println("CACHED FADER ACTOR");
      return CompletableFuture.completedStage(faderRef);
  }

  private CompletionStage<ActorRef<FaderActor.Command>> lookupFaderActor() {
      return AskPattern
        .<Receptionist.Command, Receptionist.Listing>ask(
          system.receptionist(),
          replyTo -> Receptionist.find(FaderActor.SERVICE_KEY, replyTo),
          Duration.ofSeconds(5),
          system.scheduler()
        )
        .thenApply(listing -> {
          AtomicReference<ActorRef<FaderActor.Command>> result = new AtomicReference<>(null);
          listing.getServiceInstances(FaderActor.SERVICE_KEY).stream().findFirst().ifPresent(result::set);
          return Optional.ofNullable(result.get());
        })
        .thenCompose(result -> {
          if (!result.isPresent()) return CompletableFuture.failedStage(new Exception("Cannot find actor"));

          this.faderRef = result.get();
          return CompletableFuture.completedStage(result.get());
        });
  }
}
