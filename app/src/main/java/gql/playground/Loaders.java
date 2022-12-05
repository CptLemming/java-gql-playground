package gql.playground;

import java.util.concurrent.CompletableFuture;

import org.dataloader.BatchLoaderContextProvider;
import org.dataloader.BatchLoaderWithContext;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderOptions;
import org.dataloader.DataLoaderRegistry;
import org.dataloader.ValueCache;

import akka.actor.typed.ActorSystem;
import gql.playground.enums.PathType;
import gql.playground.loaders.FaderLoader;
import gql.playground.loaders.IsAccessedLoader;
import gql.playground.models.Fader;
import graphql.schema.DataFetchingEnvironment;
import io.reactivex.rxjava3.core.Observable;

public class Loaders {
  final DataLoaderRegistry registry;

  public Loaders(ActorSystem<Void> system) {
    this.registry = build(system);
  }

  private DataLoaderRegistry build(ActorSystem<Void> system) {
    BatchLoaderWithContext<String, Boolean> isAccessedLoader = new IsAccessedLoader();
    BatchLoaderWithContext<PathType, Observable<Fader>> fadersBatchLoader = new FaderLoader();

    BatchLoaderContextProvider contextProvider = new BatchLoaderContextProvider() {
        @Override
        public Object getContext() {
            return new Context(system);
        }
    };

    ValueCache<PathType, Observable<Fader>> fadersValueCache = new ValueCache<PathType, Observable<Fader>>() {
        @Override
        public CompletableFuture<Observable<Fader>> get(PathType key) {
            System.out.println("CACHE GET :: "+ key);
            return CompletableFuture.failedFuture(new Exception("Cache is empty"));
        }

        @Override
        public CompletableFuture<Observable<Fader>> set(PathType key, Observable<Fader> value) {
          System.out.println("CACHE SET :: "+ key + " -> "+ value);
            return CompletableFuture.failedFuture(new Exception("Caching not supported"));
        }
    
        @Override
        public CompletableFuture<Void> delete(PathType key) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> clear() {
            return CompletableFuture.completedFuture(null);
        }
    };

    DataLoaderOptions fadersLoaderOptions = DataLoaderOptions.newOptions()
      .setBatchLoaderContextProvider(contextProvider)
      .setValueCache(fadersValueCache);
    DataLoaderOptions isAccessedLoaderOptions = DataLoaderOptions.newOptions()
      .setBatchLoaderContextProvider(contextProvider);
    DataLoader<PathType, Observable<Fader>> fadersDataLoader = DataLoaderFactory.newDataLoader(fadersBatchLoader, fadersLoaderOptions);
    DataLoader<String, Boolean> isAccessedDataLoader = DataLoaderFactory.newDataLoader(isAccessedLoader, isAccessedLoaderOptions);
    DataLoaderRegistry registry = new DataLoaderRegistry();
    registry.register(DataLoaders.FADERS.name(), fadersDataLoader);
    registry.register(DataLoaders.ACCESSED.name(), isAccessedDataLoader);

    return registry;
  }

  public DataLoaderRegistry getRegistry() {
    return registry;
  }

  public static enum DataLoaders {
      FADERS,
      ACCESSED
  }

  public static DataLoader<PathType, Observable<Fader>> getFadersLoader(DataFetchingEnvironment environment) {
      return environment.getDataLoader(DataLoaders.FADERS.name());
  }

  public static DataLoader<String, Boolean> getAccessedLoader(DataFetchingEnvironment environment) {
      return environment.getDataLoader(DataLoaders.ACCESSED.name());
  }
}
