package gql.playground.actors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.typesafe.config.Config;

import akka.NotUsed;
import akka.actor.ExtendedActorSystem;
import akka.actor.ActorSystem;
import akka.persistence.query.EventEnvelope;
import akka.persistence.query.NoOffset;
import akka.persistence.query.Offset;
import akka.persistence.query.ReadJournalProvider;
import akka.persistence.query.Sequence;
import akka.serialization.Serialization;
import akka.serialization.SerializationExtension;
import akka.stream.ActorAttributes;
import akka.stream.Attributes;
import akka.stream.Outlet;
import akka.stream.SourceShape;
import akka.stream.javadsl.Source;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import akka.stream.stage.TimerGraphStageLogic;

public class MyEventsByTagSource extends GraphStage<SourceShape<EventEnvelope>> {
  public Outlet<EventEnvelope> out = Outlet.create("MyEventByTagSource.out");
  private static final String QUERY =
      "SELECT id, persistence_id, seq_nr, serializer_id, serializer_manifest, payload "
          + "FROM journal WHERE tag = ? AND id > ? "
          + "ORDER BY id LIMIT ?";

  enum Continue {
    INSTANCE;
  }

  private static final int LIMIT = 1000;
  private final Connection connection;
  private final String tag;
  private final long initialOffset;
  private final Duration refreshInterval;

  // assumes a shared connection, could also be a factory for creating connections/pool
  public MyEventsByTagSource(
      Connection connection, String tag, long initialOffset, Duration refreshInterval) {
    this.connection = connection;
    this.tag = tag;
    this.initialOffset = initialOffset;
    this.refreshInterval = refreshInterval;
  }

  @Override
  public Attributes initialAttributes() {
    return Attributes.apply(ActorAttributes.IODispatcher());
  }

  @Override
  public SourceShape<EventEnvelope> shape() {
    return SourceShape.of(out);
  }

  @Override
  public GraphStageLogic createLogic(Attributes inheritedAttributes) {
    return new TimerGraphStageLogic(shape()) {
      private ActorSystem system = materializer().system();
      private long currentOffset = initialOffset;
      private List<EventEnvelope> buf = new LinkedList<>();
      private final Serialization serialization = SerializationExtension.get(system);

      @Override
      public void preStart() {
        scheduleWithFixedDelay(Continue.INSTANCE, refreshInterval, refreshInterval);
      }

      @Override
      public void onTimer(Object timerKey) {
        query();
        deliver();
      }

      private void deliver() {
        if (isAvailable(out) && !buf.isEmpty()) {
          push(out, buf.remove(0));
        }
      }

      private void query() {
        if (buf.isEmpty()) {

          try (PreparedStatement s = connection.prepareStatement(QUERY)) {
            s.setString(1, tag);
            s.setLong(2, currentOffset);
            s.setLong(3, LIMIT);
            try (ResultSet rs = s.executeQuery()) {
              final List<EventEnvelope> res = new ArrayList<>(LIMIT);
              while (rs.next()) {
                Object deserialized =
                    serialization
                        .deserialize(
                            rs.getBytes("payload"),
                            rs.getInt("serializer_id"),
                            rs.getString("serializer_manifest"))
                        .get();
                currentOffset = rs.getLong("id");
                res.add(
                    new EventEnvelope(
                        Offset.sequence(currentOffset),
                        rs.getString("persistence_id"),
                        rs.getLong("seq_nr"),
                        deserialized,
                        System.currentTimeMillis()));
              }
              buf = res;
            }
          } catch (Exception e) {
            failStage(e);
          }
        }
      }

      {
        setHandler(
            out,
            new AbstractOutHandler() {
              @Override
              public void onPull() {
                query();
                deliver();
              }
            });
      }
    };
  }
}