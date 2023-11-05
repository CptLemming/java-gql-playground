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

public class MyScaladslReadJournal
implements akka.persistence.query.scaladsl.ReadJournal,
    akka.persistence.query.scaladsl.EventsByTagQuery,
    akka.persistence.query.scaladsl.EventsByPersistenceIdQuery,
    akka.persistence.query.scaladsl.PersistenceIdsQuery,
    akka.persistence.query.scaladsl.CurrentPersistenceIdsQuery {

private final MyJavadslReadJournal javadslReadJournal;

public MyScaladslReadJournal(MyJavadslReadJournal javadslReadJournal) {
this.javadslReadJournal = javadslReadJournal;
}

@Override
public akka.stream.scaladsl.Source<EventEnvelope, NotUsed> eventsByTag(
  String tag, akka.persistence.query.Offset offset) {
return javadslReadJournal.eventsByTag(tag, offset).asScala();
}

@Override
public akka.stream.scaladsl.Source<EventEnvelope, NotUsed> eventsByPersistenceId(
  String persistenceId, long fromSequenceNr, long toSequenceNr) {
return javadslReadJournal
    .eventsByPersistenceId(persistenceId, fromSequenceNr, toSequenceNr)
    .asScala();
}

@Override
public akka.stream.scaladsl.Source<String, NotUsed> persistenceIds() {
return javadslReadJournal.persistenceIds().asScala();
}

@Override
public akka.stream.scaladsl.Source<String, NotUsed> currentPersistenceIds() {
return javadslReadJournal.currentPersistenceIds().asScala();
}

// possibility to add more plugin specific queries

public akka.stream.scaladsl.Source<RichEvent, QueryMetadata> byTagsWithMeta(
  scala.collection.Set<String> tags) {
Set<String> jTags = scala.collection.JavaConverters.setAsJavaSetConverter(tags).asJava();
return javadslReadJournal.byTagsWithMeta(jTags).asScala();
}
}