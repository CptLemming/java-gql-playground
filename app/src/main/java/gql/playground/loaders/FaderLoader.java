package gql.playground.loaders;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.dataloader.BatchLoader;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.BatchLoaderWithContext;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import gql.playground.Context;
import gql.playground.actors.FaderActor;
import gql.playground.models.Fader;
import io.reactivex.rxjava3.core.Observable;

public class FaderLoader implements BatchLoaderWithContext<String, Observable<Fader>> {

  @Override
  public CompletionStage<List<Observable<Fader>>> load(List<String> keys, BatchLoaderEnvironment loaderContext) {
    System.out.println("FADERS Loader :: "+ keys);
    Context ctx = loaderContext.getContext();
      ActorSystem<Void> system = ctx.getSystem();

      return ctx.getFaderActor()
        .thenCompose(actorRef -> {
          return AskPattern
            .ask(
              actorRef,
              replyTo -> new FaderActor.FetchFaders(keys, replyTo),
              Duration.ofSeconds(5),
              system.scheduler()
            );
      });
  }
};
