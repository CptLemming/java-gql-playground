package gql.playground;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class RxApp {
  public static int NUM_ITEMS = 255;
  // public static void main(String[] args) {
  //   List<Flowable<?>> completedResultsFlowable = new ArrayList<>(0);

  //   for (int i = 0; i < NUM_ITEMS; i++) {
  //     completedResultsFlowable.add(Flowable.just(i));
  //   }

  //   Flowable.merge(completedResultsFlowable).blockingSubscribe(res -> {
  //     System.out.println("Results -> "+ res);
  //   });
  // }

  public static void main(String[] args) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    PublishSubject<Integer> numbers = PublishSubject.create();
    PublishSubject<Integer> numbers2 = PublishSubject.create();

    // numbers.subscribe(msg -> {
    //   System.out.println("NUM 1 -> "+ msg);
    // });

    numbers2.subscribe(msg -> {
      System.out.println("NUM 2 -> "+ msg);
    });

    numbers2.onNext(2);
    numbers.onNext(1);
    numbers.onError(new RuntimeException("Ayo"));

    latch.await(10, TimeUnit.SECONDS);
  }
}
