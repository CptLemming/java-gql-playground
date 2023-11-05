package gql.playground;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class MetricsPlayground {
  static final MetricRegistry metrics = new MetricRegistry();
  static final List<Integer> queue = new ArrayList<Integer>();
  public static void main(String[] args) {
    startReport();
    Meter requests = metrics.meter("requests");
    requests.mark();

    metrics.register(MetricRegistry.name(MetricsPlayground.class, "add", "size"),
                         new Gauge<Integer>() {
                             @Override
                             public Integer getValue() {
                                 return queue.size();
                             }
                         });

    queue.add(1);
    queue.add(2);
    queue.add(3);
    wait5Seconds();
  }

  static void startReport() {
      ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();
      reporter.start(1, TimeUnit.SECONDS);
  }

  static void wait5Seconds() {
      try {
          Thread.sleep(5*1000);
      }
      catch(InterruptedException e) {}
  }
}
