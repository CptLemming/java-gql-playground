package gql.playground.actors;

import java.util.ArrayList;
import java.util.List;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import gql.playground.enums.PathType;
import gql.playground.models.Fader;
import io.reactivex.rxjava3.core.Observable;

public class FaderActor extends AbstractBehavior<FaderActor.Command> {
  public static final ServiceKey<Command> SERVICE_KEY = ServiceKey.create(Command.class, "fader-service");
  
  public static interface Command {}

  public static record FetchFaders(List<String> faders, ActorRef<List<Observable<Fader>>> replyTo) implements Command {};

  public static Behavior<Command> create() {
    return Behaviors.<Command>setup(context -> {
        System.out.println("REGISTER FADERS ACTOR");
        context.getSystem().receptionist().tell(Receptionist.register(SERVICE_KEY, context.getSelf()));
        return new FaderActor(context);
      });
  }

  private FaderActor(
    ActorContext<Command> context
  ) {
    super(context);
  }

  @Override
  public Receive<Command> createReceive() {
    return newReceiveBuilder()
      .onMessage(FetchFaders.class, this::onMessage)
      .build();
  }

  private Behavior<Command> onMessage(FetchFaders request) {
    List<Observable<Fader>> data = new ArrayList<>();
    System.out.println("FADERS Actor :: "+ request.faders);

    for (int i = 1; i <= request.faders.size(); i++) {
        String name = String.format("F%d", i);
        data.add(Observable.just(new Fader(request.faders.get(i - 1), name, PathType.CHANNEL)));
    }

    request.replyTo.tell(data);

    return Behaviors.same();
  }
}
