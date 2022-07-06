package nl.knaw.huc.di.kabara.sync;

import nl.knaw.huc.di.kabara.KabaraConfiguration;
import nl.knaw.huc.di.kabara.endpoints.GraphDB;
import nl.knaw.huc.di.kabara.endpoints.TimbuctooEndpoint;
import nl.knaw.huc.di.kabara.endpoints.TripleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.UriBuilder.fromUri;

public abstract class SyncResource {
  private static final Logger LOG = LoggerFactory.getLogger(SyncResource.class);

  private final ExecutorService executor;
  private final SyncManager syncManager;
  private final List<TimbuctooEndpoint> timbuctooEndpoints;
  private final List<TripleStore> tripleStores;
  private final List<GraphDB> graphDBs;

  private final String publicUrl;
  private final String path;

  protected SyncResource(ExecutorService executor, KabaraConfiguration configuration, String path) {
    this.executor = executor;
    this.syncManager = configuration.getSyncManager();
    this.timbuctooEndpoints = configuration.getTimbuctooEndpoints();
    this.tripleStores = configuration.getTripleStores();
    this.graphDBs = configuration.getGraphDBs();
    this.publicUrl = configuration.getPublicUrl();
    this.path = path;
  }

  protected Response ifSyncExists(Sync sync, SyncResponse syncResponse) throws IOException {
    if (!syncManager.syncExists(sync)) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    return syncResponse.getSyncResponse();
  }

  protected Response getSync(Sync sync) throws IOException {
    return ifSyncExists(sync, () -> {
      Sync reloadedSync = syncManager.reloadSync(sync);
      return Response.ok(reloadedSync).build();
    });
  }

  protected Response updateSync(Sync sync, Function<UriBuilder, URI> uriBuilder) throws IOException {
    syncManager.updateSync(sync);
    return created(uriBuilder.apply(fromUri(publicUrl).path(path))).build();
  }

  protected Response startSync(Sync sync, Function<UriBuilder, URI> uriBuilder) throws IOException {
    return ifSyncExists(sync, () -> {
      Sync reloadedSync = syncManager.reloadSync(sync);
      executor.submit(reloadedSync.createRunnable(syncManager));
      return created(uriBuilder.apply(fromUri(publicUrl).path(path))).build();
    });
  }

  protected <S extends Sync> Response createResponseFor(Supplier<S> getSync, WithSyncRequest<S> withSync) {
    try {
      S sync = getSync.get();
      if (sync != null) {
        return withSync.withSync(sync);
      }

      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (IOException e) {
      LOG.error("Failed to read/write sync file.", e);
      return Response.serverError().build();
    }
  }

  protected Optional<TimbuctooEndpoint> findEndpoint(String endpoint) {
    return timbuctooEndpoints.stream().filter(ep -> ep.getId().equals(endpoint)).findFirst();
  }

  protected Optional<TripleStore> findTripleStore(String tripleStore) {
    return tripleStores.stream().filter(ep -> ep.getId().equals(tripleStore)).findFirst();
  }

  protected Optional<GraphDB> findGraphDb(String graphDb) {
    return graphDBs.stream().filter(ep -> ep.getId().equals(graphDb)).findFirst();
  }

  @FunctionalInterface
  protected interface SyncResponse {
    Response getSyncResponse() throws IOException;
  }

  @FunctionalInterface
  protected interface WithSyncRequest<S extends Sync> {
    Response withSync(S sync) throws IOException;
  }
}
