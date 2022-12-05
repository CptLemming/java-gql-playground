package gql.playground;

import org.dataloader.BatchLoaderContextProvider;
import org.dataloader.BatchLoaderWithContext;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderOptions;
import org.dataloader.DataLoaderRegistry;

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

    DataLoaderOptions loaderOptions = DataLoaderOptions.newOptions().setBatchLoaderContextProvider(contextProvider);
    DataLoader<PathType, Observable<Fader>> fadersDataLoader = DataLoaderFactory.newDataLoader(fadersBatchLoader, loaderOptions);
    DataLoader<String, Boolean> taxDataLoader = DataLoaderFactory.newDataLoader(isAccessedLoader, loaderOptions);
    DataLoaderRegistry registry = new DataLoaderRegistry();
    registry.register(DataLoaders.FADERS.name(), fadersDataLoader);
    registry.register(DataLoaders.ACCESSED.name(), taxDataLoader);

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
