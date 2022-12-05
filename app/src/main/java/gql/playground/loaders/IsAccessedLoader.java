package gql.playground.loaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.BatchLoaderWithContext;

import gql.playground.models.Fader;

public class IsAccessedLoader implements BatchLoaderWithContext<String, Boolean> {
  
  @Override
  public CompletionStage<List<Boolean>> load(List<String> keys, BatchLoaderEnvironment loaderContext) {
      List<Boolean> data = new ArrayList<>();
      System.out.println("ACCESSED Loader :: "+ keys);
      Map<Object, Object> keysContext = loaderContext.getKeyContexts();

      for (int i = 0; i < keys.size(); i++) {
          Fader fader = (Fader) keysContext.get(keys.get(i));
          data.add(fader.getId().equals("1"));
      }

      return CompletableFuture.completedStage(data);
  }
}