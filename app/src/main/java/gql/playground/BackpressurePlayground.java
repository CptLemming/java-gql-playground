package gql.playground;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackpressurePlayground {
  public static Logger logger = LoggerFactory.getLogger(BackpressurePlayground.class.getSimpleName());

  public static void main(String[] args) throws InterruptedException {
    PublishSubject<String> killswitch = PublishSubject.create();
    CountDownLatch latch = new CountDownLatch(1);

    Flowable<Patch> subscription1 = Flowable.create((emitter) -> {
      // logger.info("EMIT(abc123) -> "+ Thread.currentThread());
      emitter.onNext(new Patch("add", "/abc123", "{}"));
      emitter.onNext(new Patch("add", "/abc123/fader", "{}"));
      emitter.onNext(new Patch("add", "/abc123/fader/info", "{}"));
      emitter.onNext(new Patch("add", "/abc123/fader/path", "{}"));
      emitter.onNext(new Patch("add", "/abc123/fader/path/info", "{}"));
      emitter.onNext(new Patch("add", "/abc123/fader/path/info/path", "CH/0/0"));

      Thread t = new Thread(() -> {
        for (int i = 0; i < 10; i++) {
          emitter.onNext(new Patch("add", "/abc123/fader/info/level", i));
          try {
            Thread.sleep(250);
          } catch (InterruptedException e) {
          }
        }

        emitter.onComplete();
      });

      t.run();
    }, BackpressureStrategy.BUFFER);

    Flowable<Patch> subscription2 = Flowable.create((emitter) -> {
      // logger.info("EMIT(dev456) -> "+ Thread.currentThread());
      emitter.onNext(new Patch("add", "/def456", "{}"));
      emitter.onNext(new Patch("add", "/def456/fader", "{}"));
      emitter.onNext(new Patch("add", "/def456/fader/path", "{}"));
      emitter.onNext(new Patch("add", "/def456/fader/path/input", "{}"));

      for (int i = 0; i < 100; i++) {
        emitter.onNext(new Patch("add", "/def456/fader/path/input/trim", i));
      }

      emitter.onComplete();
    }, BackpressureStrategy.BUFFER);

    Flowable<Patch> subscription3 = Flowable.create((emitter) -> {
      // logger.info("EMIT(dev456) -> "+ Thread.currentThread());
      emitter.onNext(new Patch("add", "/ghj789", "{}"));
      emitter.onNext(new Patch("add", "/ghj789/fader", "{}"));
      emitter.onNext(new Patch("add", "/ghj789/fader/info", "{}"));
      emitter.onNext(new Patch("add", "/ghj789/fader/info/isAccess", true));
      emitter.onComplete();
    }, BackpressureStrategy.BUFFER);

    Flowable<Patch> subscription4 = Flowable.create((emitter) -> {
      // logger.info("EMIT(dev456) -> "+ Thread.currentThread());
      emitter.onNext(new Patch("add", "/items", "[]"));
      emitter.onNext(new Patch("add", "/items/0", "{}"));
      emitter.onNext(new Patch("add", "/items/1", "{}"));
      emitter.onNext(new Patch("add", "/items/0/info", "{}"));
      emitter.onNext(new Patch("add", "/items/1/info", "{}"));
      emitter.onComplete();
    }, BackpressureStrategy.BUFFER);

    Flowable
      .merge(
        subscription1,
        subscription2,
        subscription3.delay(1500, TimeUnit.MILLISECONDS),
        subscription4
      )
      // .doOnNext(item -> {
      //   logger.info("EMIT -> "+ item);
      // })

      .takeUntil(killswitch.toFlowable(BackpressureStrategy.LATEST))
      .map(item -> PatchOperations.wrap(item))
      .doOnCancel(() -> {
        logger.info("CANCEL");
      })
      .onBackpressureReduce((coll, item) -> {
          // logger.info("REDUCE -> "+ coll + " or "+ item);
          return coll.merge(item);
      })
      .observeOn(Schedulers.single(), false, 1)
      .subscribeOn(Schedulers.io())
      .doOnCancel(() -> {
        logger.info("CANCEL 2");
      })
      .subscribe(new Subscriber<>() {
        public static Logger logger = LoggerFactory.getLogger(BackpressurePlayground.class.getSimpleName() + "#Subscriber");
        Subscription s;

        @Override
        public void onSubscribe(Subscription s) {
          this.s = s;
          logger.info("START");
          s.request(1);
          // s.cancel();
          // killswitch.onComplete();
        }

        @Override
        public void onNext(PatchOperations item) {
          logger.info("NEXT -> "+ item);
          try {
            // Fake sending over websocket, wait for response
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          // logger.info("REQ");
          s.request(1);
        }

        @Override
        public void onError(Throwable e) {
          logger.error("ERR -> "+ e.getMessage());
        }

        @Override
        public void onComplete() {
          logger.info("COMPLETE");
          latch.countDown();
        }
      });

      latch.await();
  }

  record Patch(String op, String path, Object value) {}
  static class PatchOperations {
    private LinkedHashMap<String, Patch> cache = new LinkedHashMap<>();

    public PatchOperations() {}

    public static PatchOperations wrap(Patch item) {
      var collection = new PatchOperations();
      collection.add(item);
      return collection;
    }

    public void add(Patch item) {
      this.cache.put(item.path, item);
    }

    public List<Patch> items() {
      return new ArrayList<>(cache.values());
    }

    public HashMap<String, Patch> getCache() {
      return cache;
    }

    public PatchOperations merge(PatchOperations collection) {
      this.cache.putAll(collection.getCache());
      return this;
    }

    public List<Patch> getValues() {
      return cache
        .values()
        .stream()
        .sorted(
          Comparator.comparingInt(patch -> {
            return patch.path.length();
          })
        )
        .toList();
    }

    @Override
    public String toString() {
      return "Collection[\n "+ (
        getValues()
        .stream()
        .sorted(Comparator.comparingInt(patch -> patch.path.length()))
        .map(entry -> entry.toString())
        .collect(Collectors.joining(",\n "))
      ) +"]";
    }
  }
}
