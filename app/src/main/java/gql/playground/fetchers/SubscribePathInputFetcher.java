package gql.playground.fetchers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;

import gql.playground.models.PathInput;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class SubscribePathInputFetcher implements DataFetcher<Publisher<PathInput>> {
  @Override
  public Publisher<PathInput> get(DataFetchingEnvironment environment) {
    PublishSubject<PathInput> subject = PublishSubject.create();

    Thread thread1 = new Thread("PathInputThread-1"){
      public void run(){
        // System.out.println("Thread Running");
        Observable.interval(5, TimeUnit.MILLISECONDS).blockingSubscribe(next -> subject.onNext(new PathInput(100L)));
      }
    };
    Thread thread2 = new Thread("PathInputThread-2"){
      public void run(){
        // System.out.println("Thread Running");
        Observable.interval(6, TimeUnit.MILLISECONDS).blockingSubscribe(next -> subject.onNext(new PathInput(200L)));
      }
    };
    Thread thread3 = new Thread("PathInputThread-3"){
      public void run(){
        // System.out.println("Thread Running");
        Observable.interval(4, TimeUnit.MILLISECONDS).blockingSubscribe(next -> subject.onNext(new PathInput(300L)));
      }
    };
    Thread thread4 = new Thread("PathInputThread-4"){
      public void run(){
        // System.out.println("Thread Running");
        Observable.interval(8, TimeUnit.MILLISECONDS).blockingSubscribe(next -> subject.onNext(new PathInput(400L)));
      }
    };

      return subject.doOnSubscribe(d -> {
          thread1.start();
          thread2.start();
          thread3.start();
          thread4.start();
        })
        .doOnDispose(() -> {
          thread1.stop();
          thread2.stop();
          thread3.stop();
          thread4.stop();
        })
        .observeOn(Schedulers.single(), false, 16)
        .toFlowable(BackpressureStrategy.LATEST);
  }
}
