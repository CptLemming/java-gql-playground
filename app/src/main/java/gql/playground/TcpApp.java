package gql.playground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletionStage;

import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.japi.Pair;
import akka.stream.javadsl.Tcp;
import akka.stream.javadsl.Tcp.OutgoingConnection;
import akka.util.ByteString;
import akka.stream.KillSwitches;
import akka.stream.UniqueKillSwitch;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Framing;
import akka.stream.javadsl.FramingTruncation;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Source;

public class TcpApp {
  public static void main(String[] args) {

    System.out.println("Starting...");
    TcpApp.netstat();

    ActorSystem<?> system = ActorSystem.create(Behaviors.empty(), "root");
    final Flow<ByteString, ByteString, Pair<CompletionStage<OutgoingConnection>, UniqueKillSwitch>> connection = Tcp.get(system)
        .outgoingConnection("127.0.0.1", 3000)
        .viaMat(KillSwitches.single(), Keep.both());

    final Flow<String, ByteString, NotUsed> replParser = Flow.<String>create()
        .takeWhile(elem -> !elem.equals("q"))
        .concat(Source.single("BYE")) // will run after the original flow completes
        .map(elem -> ByteString.fromString(elem + "\n"));

    final Flow<ByteString, ByteString, NotUsed> repl = Flow.of(ByteString.class)
        .via(Framing.delimiter(ByteString.fromString("\n"), 256, FramingTruncation.DISALLOW))
        .map(ByteString::utf8String)
        .map(
            text -> {
              System.out.println("Server: " + text);
              return "next";
            })
        .map(elem -> readLine("> "))
        .via(replParser);

    Pair<CompletionStage<OutgoingConnection>, UniqueKillSwitch> connectionCS = connection
      .join(repl)
      .run(system);

    connectionCS.first().whenComplete((result, error) -> {
      System.out.println("Result");
      System.out.println(result);
      System.out.println("Error");
      System.out.println(error);
      TcpApp.netstat();

      TimerTask task = new TimerTask() {
        public void run() {
          System.out.println("Timer expired");
          connectionCS.second().shutdown();
          TcpApp.netstat();
        }
      };
      Timer timer = new Timer("Timer");

      timer.schedule(task, 2000L);
    });
  }

  public static String readLine(String chars) {
    return "woof";
  }

  public static void netstat() {
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.command("netstat", "-an | grep 3000");

    try {
      Process process = processBuilder.start();
      StringBuilder output = new StringBuilder();
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line + "\n");
      }

      int exitVal = process.waitFor();

      System.out.println(output);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
