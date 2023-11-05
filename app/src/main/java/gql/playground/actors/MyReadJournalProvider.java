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

public class MyReadJournalProvider implements ReadJournalProvider {
  private final MyJavadslReadJournal javadslReadJournal;

  public MyReadJournalProvider(ExtendedActorSystem system, Config config) {
    this.javadslReadJournal = new MyJavadslReadJournal(system, config);
  }

  @Override
  public MyScaladslReadJournal scaladslReadJournal() {
    return new MyScaladslReadJournal(javadslReadJournal);
  }

  @Override
  public MyJavadslReadJournal javadslReadJournal() {
    return this.javadslReadJournal;
  }
}