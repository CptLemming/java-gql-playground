package gql.playground;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.japi.Pair;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.typed.javadsl.ActorSource;
import akka.util.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class BroadcastHubs {
  private static Integer NUM_MESSAGES = 50;

  public static void main(String[] args) {
    Behavior<ParentCommand> parent = Behaviors.setup(context -> {
      BlockingQueue<ByteString> buffer = new ArrayBlockingQueue<ByteString>(1024);
      AtomicBoolean isBusy = new AtomicBoolean(false);

      Source<ByteString, ActorRef<ByteString>> mcsOutbound = ActorSource.actorRefWithBackpressure(
        context.getSelf(),
        StreamState.ACK,
        msg -> Optional.empty(),
        msg -> Optional.empty()
      );

      Pair<ActorRef<ByteString>, Source<ByteString, NotUsed>> outboundPair = mcsOutbound.preMaterialize(context.getSystem());

      Sink<ByteString, CompletionStage<Done>> sink = Sink.foreach(message -> {
        System.out.println(">>>>> MCS MSG -> "+ message.toString() + " on thread : "+ Thread.currentThread());
      });

      outboundPair.second()
        .log("MCSClientHub")
        .runWith(sink, context.getSystem());

      return Behaviors.receive(ParentCommand.class)
        .onMessage(SendExternalMessage.class, (command) -> {
          System.out.println("Incoming -> "+ command.index + " on thread : "+ Thread.currentThread());
          ByteBuf out = Unpooled.buffer().order(LITTLE_ENDIAN);
          out.writeByte(command.index);

          buffer.add(ByteString.fromByteBuffer(out.nioBuffer()));
          context.getSelf().tell(new SendNextMessage());

          return Behaviors.same();
        })
        .onMessage(SendMultiMessage.class, (command) -> {
          System.out.println("MultiIncoming -> "+ command.indexes + " on thread : "+ Thread.currentThread());
          List<ByteString> messages = command.indexes.stream()
            .map(index -> {
              ByteBuf out = Unpooled.buffer().order(LITTLE_ENDIAN);
              out.writeByte(index);
              return ByteString.fromByteBuffer(out.nioBuffer());
            }).toList();

          buffer.addAll(messages);
          context.getSelf().tell(new SendNextMessage());

          return Behaviors.same();
        })
        .onMessage(SendNextMessage.class, ignore -> {
          if (isBusy.get() == false) {
            Optional.ofNullable(buffer.poll())
              .ifPresent(message -> {
                outboundPair.first().tell(message);
              });
            isBusy.set(true);
          }

          return Behaviors.same();
        })
        .onMessage(StreamState.class, ignore -> {
          isBusy.set(false);
          context.getSelf().tell(new SendNextMessage());
          return Behaviors.same();
        })
        .build();
    });

    Behavior<Done> root = Behaviors.setup(context -> {
      System.out.println("CREATE ACTOR SYSTEM");
      ActorRef<ParentCommand> parentActorRef = context.spawn(parent, "parent");

      for (int i = 0; i < NUM_MESSAGES; i++) {
        parentActorRef.tell(new SendExternalMessage(i));
      }
      parentActorRef.tell(new SendMultiMessage(Arrays.asList(100, 101, 102, 103, 104, 105)));
      return Behaviors.empty();
    });
    ActorSystem.create(root, "root");
  }

  public static interface ParentCommand {}
  public static record SendExternalMessage(Integer index) implements ParentCommand {}
  public static record SendMultiMessage(List<Integer> indexes) implements ParentCommand {}
  public static record MessageProcessed() implements ParentCommand {}
  public static record Stop() implements ParentCommand {}
  public static record SendNextMessage() implements ParentCommand {}
  public static enum StreamState implements ParentCommand {
    ACK
  }
}
