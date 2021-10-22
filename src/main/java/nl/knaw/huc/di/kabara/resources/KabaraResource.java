package nl.knaw.huc.di.kabara.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;
import nl.knaw.huc.di.kabara.run.DatasetRunnableFactory;
import nl.knaw.huc.di.kabara.dataset.Dataset;
import nl.knaw.huc.di.kabara.dataset.DatasetManager;
import nl.knaw.huc.di.kabara.dataset.TimbuctooEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.UriBuilder.fromUri;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class KabaraResource {
  private static final Logger LOG = LoggerFactory.getLogger(KabaraResource.class);

  private final ExecutorService executor;
  private final DatasetRunnableFactory datasetRunnableFactory;
  private final DatasetManager datasetManager;
  private final List<TimbuctooEndpoint> timbuctooEndpoints;
  private final String publicUrl;

  public KabaraResource(ExecutorService executor, DatasetRunnableFactory datasetRunnableFactory,
                        DatasetManager datasetManager,
                        List<TimbuctooEndpoint> timbuctooEndpoints, String publicUrl) {
    this.executor = executor;
    this.datasetRunnableFactory = datasetRunnableFactory;
    this.datasetManager = datasetManager;
    this.timbuctooEndpoints = timbuctooEndpoints;
    this.publicUrl = publicUrl;
  }

  @GET
  public String checkActive() {
    return "Kabara active";
  }

  @GET
  @Path("{endpoint}/{userId}/{datasetName}")
  @Produces(MediaType.APPLICATION_JSON)
  @JacksonFeatures(
      serializationEnable = {SerializationFeature.INDENT_OUTPUT},
      serializationDisable = {SerializationFeature.WRITE_DATES_AS_TIMESTAMPS})
  public Response getDataset(@PathParam("endpoint") String endpoint,
                             @PathParam("userId") String userId,
                             @PathParam("datasetName") String datasetName) {
    try {
      Optional<TimbuctooEndpoint> timbuctooEndpoint = findEndpoint(endpoint);
      if (timbuctooEndpoint.isPresent()) {
        final Optional<Dataset> dataset = datasetManager.getDataset(timbuctooEndpoint.get(), userId, datasetName);
        if (dataset.isPresent()) {
          return Response.ok(dataset).build();
        }
      }

      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (IOException e) {
      LOG.error("Failed to read dataset file.", e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path("{endpoint}/{userId}/{datasetName}")
  @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
  public Response setDatasetProperties(@PathParam("endpoint") String endpoint,
                                       @PathParam("userId") String userId,
                                       @PathParam("datasetName") String datasetName,
                                       @FormParam("graphUri") String graphUri,
                                       @FormParam("autoSync") boolean autoSync) {
    try {
      Optional<TimbuctooEndpoint> timbuctooEndpoint = findEndpoint(endpoint);
      if (timbuctooEndpoint.isPresent()) {
        final Dataset dataset = datasetManager.getDatasetOrCreate(timbuctooEndpoint.get(), userId, datasetName);

        dataset.updateGraphUri(graphUri);
        dataset.updateAutoSync(autoSync);
        datasetManager.updateDataset(dataset);

        return created(fromUri(publicUrl).path(endpoint).path(userId).path(datasetName).build()).build();
      }

      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (IOException e) {
      LOG.error("Failed to read/write dataset file.", e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path("{endpoint}/sync")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response autoSyncDataset(@PathParam("endpoint") String endpoint, Sync sync) {
    try {
      Optional<TimbuctooEndpoint> timbuctooEndpoint = findEndpoint(endpoint);
      if (timbuctooEndpoint.isPresent()) {
        final Optional<Dataset> dataset =
            datasetManager.getDataset(timbuctooEndpoint.get(), sync.getUserId(), sync.getDatasetName());
        if (dataset.isPresent() && dataset.get().isAutoSync()) {
          executor.submit(datasetRunnableFactory.createRunnable(dataset.get()));
          return created(fromUri(publicUrl).path(endpoint).path(sync.getUserId())
                                           .path(sync.getDatasetName()).build()).build();
        }
      }

      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (IOException e) {
      LOG.error("Failed to read dataset file.", e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path("{endpoint}/{userId}/{datasetName}/sync")
  public Response syncDataset(@PathParam("endpoint") String endpoint,
                              @PathParam("userId") String userId,
                              @PathParam("datasetName") String datasetName) {
    try {
      Optional<TimbuctooEndpoint> timbuctooEndpoint = findEndpoint(endpoint);
      if (timbuctooEndpoint.isPresent()) {
        final Optional<Dataset> dataset = datasetManager.getDataset(timbuctooEndpoint.get(), userId, datasetName);
        if (dataset.isPresent()) {
          executor.submit(datasetRunnableFactory.createRunnable(dataset.get()));
          return created(fromUri(publicUrl).path(endpoint).path(userId).path(datasetName).build()).build();
        }
      }

      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (IOException e) {
      LOG.error("Failed to read dataset file.", e);
      return Response.serverError().build();
    }
  }

  private Optional<TimbuctooEndpoint> findEndpoint(String endpoint) {
    return timbuctooEndpoints.stream().filter(ep -> ep.getId().equals(endpoint)).findFirst();
  }

  private static class Sync {
    private final String dataSetId;

    @JsonCreator
    public Sync(@JsonProperty("dataSetId") String dataSetId) {
      this.dataSetId = dataSetId;
    }

    public String getUserId() {
      return dataSetId.split("__", 2)[0];
    }

    public String getDatasetName() {
      return dataSetId.split("__", 2)[1];
    }
  }
}
