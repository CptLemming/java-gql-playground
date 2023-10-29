package gql.playground;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import akka.japi.Pair;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.observables.GroupedObservable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class RxSpeed {
  private static Integer NUM_WARMUP = 3;
  private static Integer NUM_TIMES = 10;
  private static Integer NUM_ITERATIONS = 1000;

  public static void main(String[] args) throws InterruptedException {
    RxSpeed app = new RxSpeed();

    List<Pair<Integer, Long>> times = new ArrayList<>();
    Long avg = 0L;

    // for (int i = 0; i < NUM_WARMUP + NUM_TIMES; i++) {
    //   long start = System.nanoTime();
    //   app.manualGrouping();
    //   long end = (System.nanoTime() - start) / 1000;
    //   times.add(Pair.create(i, end));

    //   System.out.println("manualGrouping("+ i + ") -> "+ end + "us");
    // }
    // avg = times.stream()
    //   .filter(pair -> pair.first() >= NUM_WARMUP)
    //   .map(Pair::second)
    //   .collect(Collectors.summingLong(x -> x)) / NUM_TIMES;
    // System.out.println("manualGrouping(AVG) -> "+ avg + "us");
    // times.clear();

    // for (int i = 0; i < NUM_WARMUP + NUM_TIMES; i++) {
    //   long start = System.nanoTime();
    //   app.filterSubject();
    //   long end = (System.nanoTime() - start) / 1000;
    //   times.add(Pair.create(i, end));

    //   System.out.println("filterSubject("+ i + ") -> "+ end + "us");
    // }
    // avg = times.stream()
    //   .filter(pair -> pair.first() >= NUM_WARMUP)
    //   .map(Pair::second)
    //   .collect(Collectors.summingLong(x -> x)) / NUM_TIMES;
    // System.out.println("filterSubject(AVG) -> "+ avg + "us");
    // times.clear();

    for (int i = 0; i < NUM_WARMUP + NUM_TIMES; i++) {
      long start = System.nanoTime();
      app.filterObserveOn();
      long end = (System.nanoTime() - start) / 1000;
      times.add(Pair.create(i, end));

      System.out.println("filterObserveOn("+ i + ") -> "+ end + "us");
    }
    avg = times.stream()
      .filter(pair -> pair.first() >= NUM_WARMUP)
      .map(Pair::second)
      .collect(Collectors.summingLong(x -> x)) / NUM_TIMES;
    System.out.println("filterObserveOn(AVG) -> "+ avg + "us");
    times.clear();

    // for (int i = 0; i < NUM_WARMUP + NUM_TIMES; i++) {
    //   long start = System.nanoTime();
    //   app.groupBy();
    //   long end = (System.nanoTime() - start) / 1000;
    //   times.add(Pair.create(i, end));

    //   System.out.println("groupBy("+ i + ") -> "+ end + "us");
    // }
    // avg = times.stream()
    //   .filter(pair -> pair.first() >= NUM_WARMUP)
    //   .map(Pair::second)
    //   .collect(Collectors.summingLong(x -> x)) / NUM_TIMES;
    // System.out.println("groupBy(AVG) -> "+ avg + "us");
    // times.clear();
  }

  private void filterSubject() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(NUM_ITERATIONS);
    PublishSubject<Pair<Integer, Integer>> subject = PublishSubject.create();
    CompositeDisposable disposable = new CompositeDisposable();

    for (int i = 0; i < NUM_ITERATIONS; i++) {
      final Integer index = i;
      disposable.add(subject
        .filter(msg -> {
          System.out.println("filterSubject:Filter -> "+ index + " on "+ Thread.currentThread().getName());
          return msg.first().equals(index);
        })
        .map(ignore -> {
          System.out.println("filterSubject:Map -> "+ index + " on "+ Thread.currentThread().getName());
          fibonacci(24);
          return ignore;
        })
        .subscribe(ignore -> {
          System.out.println("filterSubject:Subscribe -> "+ index + " on "+ Thread.currentThread().getName());
          fibonacci(24);
          latch.countDown();
        }));
    }

    for (int i = 0; i < NUM_ITERATIONS; i++) {
      System.out.println("filterSubject:Publish "+ i +" on "+ Thread.currentThread().getName());
      subject.onNext(Pair.create(i, i));
    }

    latch.await(30, TimeUnit.SECONDS);
    disposable.dispose();
  }

  private void filterObserveOn() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(NUM_ITERATIONS);
    PublishSubject<Pair<Integer, Integer>> subject = PublishSubject.create();
    CompositeDisposable disposable = new CompositeDisposable();

    for (int i = 0; i < NUM_ITERATIONS; i++) {
      final Integer index = i;
      disposable.add(subject
        .filter(msg -> {
          // System.out.println("filterObserveOn:Filter -> "+ index + " on "+ Thread.currentThread().getName());
          return msg.first().equals(index);
        })
        // .map(ignore -> {
        //   System.out.println("filterObserveOn:Map -> "+ index + " on "+ Thread.currentThread().getName());
        //   fibonacci(24);
        //   return ignore;
        // })
        .observeOn(Schedulers.computation())
        .subscribe(ignore -> {
          // System.out.println("filterObserveOn:Subscribe -> "+ index + " on "+ Thread.currentThread().getName());
          fibonacci(24);
          latch.countDown();
        }));
    }

    for (int i = 0; i < NUM_ITERATIONS; i++) {
      // System.out.println("filterSubject:Publish "+ i +" on "+ Thread.currentThread().getName());
      subject.onNext(Pair.create(i, i));
    }

    latch.await(30, TimeUnit.SECONDS);
    disposable.dispose();
  }

  private void manualGrouping() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(NUM_ITERATIONS);
    Map<Integer, PublishSubject<Pair<Integer, Integer>>> subjects = new HashMap<>();
    CompositeDisposable disposable = new CompositeDisposable();

    for (int i = 0; i < NUM_ITERATIONS; i++) {
      subjects.put(i, PublishSubject.create());
    }

    for (int i = 0; i < NUM_ITERATIONS; i++) {
      disposable.add(subjects.get(i).subscribe(ignore -> {
        fibonacci(24);
        latch.countDown();
      }));
    }

    for (int i = 0; i < NUM_ITERATIONS; i++) {
      subjects.get(i).onNext(Pair.create(i, i));
    }

    latch.await(30, TimeUnit.SECONDS);
    disposable.dispose();
  }

  private void groupBy() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(NUM_ITERATIONS);
    PublishSubject<Pair<Integer, Integer>> subject = PublishSubject.create();
    CompositeDisposable disposable = new CompositeDisposable();

    Observable<GroupedObservable<Integer, Pair<Integer, Integer>>> shared = subject
      .groupBy(msg -> msg.first())
      .share()
      .replay()
      .refCount();

    for (int i = 0; i < NUM_ITERATIONS; i++) {
      final Integer index = i;
      disposable.add(shared.filter(grp -> grp.getKey().equals(index)).subscribe(ignore -> {
        fibonacci(24);
        latch.countDown();
      }));
    }

    for (int i = 0; i < NUM_ITERATIONS; i++) {
      subject.onNext(Pair.create(i, i));
    }

    latch.await(30, TimeUnit.SECONDS);
    disposable.dispose();
  }

  public static Integer fibonacci(int n) {
    if (n <= 1) {
      return n;
    }
    return fibonacci(n - 1) + fibonacci(n - 2);
  }
}
