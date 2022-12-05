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
import gql.playground.enums.ProductType;
import gql.playground.models.Product;
import io.reactivex.rxjava3.core.Observable;

public class ProductActor extends AbstractBehavior<ProductActor.Command> {
  public static final ServiceKey<Command> SERVICE_KEY = ServiceKey.create(Command.class, "product-service");
  
  public static interface Command {}

  public static record FetchProducts(List<ProductType> products, ActorRef<List<Observable<Product>>> replyTo) implements Command {};

  public static Behavior<Command> create() {
    return Behaviors.<Command>setup(context -> {
        System.out.println("REGISTER PRODUCT ACTOR");
        context.getSystem().receptionist().tell(Receptionist.register(SERVICE_KEY, context.getSelf()));
        return new ProductActor(context);
      });
  }

  private ProductActor(
    ActorContext<Command> context
  ) {
    super(context);
  }

  @Override
  public Receive<Command> createReceive() {
    return newReceiveBuilder()
      .onMessage(FetchProducts.class, this::onMessage)
      .build();
  }

  private Behavior<Command> onMessage(FetchProducts request) {
    List<Product> data = new ArrayList<>();
    System.out.println("PRODUCTS Actor :: "+ request.products);

    for (int i = 1; i <= request.products.size(); i++) {
        String ID = Integer.toString(i);
        String name = String.format("P%d", i);
        data.add(new Product(ID, name, request.products.get(i - 1)));
    }

    request.replyTo.tell(data.stream().map(Observable::just).toList());

    return Behaviors.same();
  }
}
