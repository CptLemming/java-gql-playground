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

public class MyJavadslReadJournal
implements akka.persistence.query.javadsl.ReadJournal,
    akka.persistence.query.javadsl.EventsByTagQuery,
    akka.persistence.query.javadsl.EventsByPersistenceIdQuery,
    akka.persistence.query.javadsl.PersistenceIdsQuery,
    akka.persistence.query.javadsl.CurrentPersistenceIdsQuery {

private final Duration refreshInterval;
private Connection conn;

public MyJavadslReadJournal(ExtendedActorSystem system, Config config) {
refreshInterval = config.getDuration("refresh-interval");
}

/**
* You can use `NoOffset` to retrieve all events with a given tag or retrieve a subset of all
* events by specifying a `Sequence` `offset`. The `offset` corresponds to an ordered sequence
* number for the specific tag. Note that the corresponding offset of each event is provided in
* the [[akka.persistence.query.EventEnvelope]], which makes it possible to resume the stream at
* a later point from a given offset.
*
* <p>The `offset` is exclusive, i.e. the event with the exact same sequence number will not be
* included in the returned stream. This means that you can use the offset that is returned in
* `EventEnvelope` as the `offset` parameter in a subsequent query.
*/
@Override
public Source<EventEnvelope, NotUsed> eventsByTag(String tag, Offset offset) {
if (offset instanceof Sequence) {
  Sequence sequenceOffset = (Sequence) offset;
  return Source.fromGraph(
      new MyEventsByTagSource(conn, tag, sequenceOffset.value(), refreshInterval));
} else if (offset == NoOffset.getInstance())
  return eventsByTag(tag, Offset.sequence(0L)); // recursive
else
  throw new IllegalArgumentException(
      "MyJavadslReadJournal does not support " + offset.getClass().getName() + " offsets");
}

@Override
public Source<EventEnvelope, NotUsed> eventsByPersistenceId(
  String persistenceId, long fromSequenceNr, long toSequenceNr) {
// implement in a similar way as eventsByTag
throw new UnsupportedOperationException("Not implemented yet");
}

@Override
public Source<String, NotUsed> persistenceIds() {
// implement in a similar way as eventsByTag
throw new UnsupportedOperationException("Not implemented yet");
}

@Override
public Source<String, NotUsed> currentPersistenceIds() {
// implement in a similar way as eventsByTag
throw new UnsupportedOperationException("Not implemented yet");
}

// possibility to add more plugin specific queries

public Source<RichEvent, QueryMetadata> byTagsWithMeta(Set<String> tags) {
// implement in a similar way as eventsByTag
throw new UnsupportedOperationException("Not implemented yet");
}
}