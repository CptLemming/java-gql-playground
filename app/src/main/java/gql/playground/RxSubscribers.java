package gql.playground;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;

public class RxSubscribers {
  public static void main(String[] args) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Subject<Integer> subj1 = ReplaySubject.create(1);
    Subject<Integer> subj2 = ReplaySubject.create(1);

    subj1.onNext(1);
    subj2.onNext(2);

    Observable<Integer> sub1 = subj1.doOnSubscribe(disp -> {
      System.out.println("Subj1 doOnSubscribe");
    });

    Observable<Integer> sub2 = subj2.doOnSubscribe(disp -> {
      System.out.println("Subj2 doOnSubscribe");
    });

    Observable<Integer> merge = Observable.merge(sub1, sub2);

    // merge.doOnSubscribe(disp -> {
    //   System.out.println("Merge doOnSubscribe");
    // }).subscribe(msg -> {
    //   System.out.println("Merge -> "+ msg);
    // });

    latch.await(5, TimeUnit.SECONDS);
  }
}
