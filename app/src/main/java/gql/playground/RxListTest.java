package gql.playground;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class RxListTest {
  public static void main(String[] args) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    Bus main1 = new Bus(PathType.MN, Format.ST, 1, "Main 1");
    Bus main1copy = new Bus(PathType.MN, Format.ST, 1, "Main 1");

    Subject<List<Bus>> subj = PublishSubject.create();

    subj.distinctUntilChanged().subscribe(msg -> {
      System.out.println("Bus -> "+ msg);
    });

    subj.onNext(Arrays.asList(main1));
    subj.onNext(Arrays.asList(main1copy));
    subj.onNext(Arrays.asList(main1));

    latch.await();
  }

  public static class Bus {

    public PathType pathType;

    public Format format;

    public int index;

    public String name;

    public Bus() {
    }

    public Bus(PathType pathType, Format format, int index, String name) {
      this.pathType = pathType;
      this.format = format;
      this.index = index;
      this.name = name;
    }

    public Boolean isAux() {
      return pathType == PathType.AUX;
    }

    public Boolean isMain() {
      return pathType == PathType.MN;
    }

    public Boolean isGroup() {
      return pathType == PathType.GP;
    }

    public Boolean isTrack() {
      return pathType == PathType.TK;
    }

    public String getBusName() {
      if (isAux())
        return "Aux " + (index + 1);
      if (isMain())
        return "Main " + (index + 1);
      if (isGroup())
        return "Group " + (index + 1);
      if (isTrack())
        return "Track " + (index + 1);

      return name;
    }

    public String getBusShortName() {
      if (isAux())
        return "Ax " + (index + 1);
      if (isMain())
        return "Mn " + (index + 1);
      if (isGroup())
        return "Grp " + (index + 1);
      if (isTrack())
        return "Trk " + (index + 1);

      return name;
    }

    public String getBusLabel() {
      if (!getBusName().equals(name))
        return name;

      return "";
    }

    // public PathId getPath() {
    //   return new PathId(pathType, index, 0);
    // }

    public Format getFormat() {
      return format;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      Bus bus = (Bus) o;
      return index == bus.index && pathType == bus.pathType && format == bus.format && Objects.equals(name, bus.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(pathType, format, index, name);
    }
  }

  public static enum PathType {
    U,
    CH,
    GP,
    MN,
    TK,
    AUX,
    AFL,
    PFL,
    VCA,
    DIR,
    EXT,
    MM,
    AUT,
    MMO,
    OAC,
    REM_FADER,
    REM_AUX,
  }

  public static enum Format {
    NP,
    M,
    ST
  }

}
