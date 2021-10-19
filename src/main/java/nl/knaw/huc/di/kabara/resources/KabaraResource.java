package nl.knaw.huc.di.kabara.resources;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;
import nl.knaw.huc.di.kabara.run.DatasetRunnableFactory;
import nl.knaw.huc.di.kabara.status.DataSetStatus;
import nl.knaw.huc.di.kabara.status.DataSetStatusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static java.net.URLEncoder.encode;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.UriBuilder.fromUri;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class KabaraResource {
  private static final Logger LOG = LoggerFactory.getLogger(KabaraResource.class);

  private final ExecutorService executor;
  private final DatasetRunnableFactory datasetRunnableFactory;
  private final DataSetStatusManager dataSetStatusManager;
  private final String publicUrl;

  public KabaraResource(ExecutorService executor, DatasetRunnableFactory datasetRunnableFactory,
                        DataSetStatusManager dataSetStatusManager, String publicUrl) {
    this.executor = executor;
    this.datasetRunnableFactory = datasetRunnableFactory;
    this.dataSetStatusManager = dataSetStatusManager;
    this.publicUrl = publicUrl;
  }

  @GET
  public String checkActive() {
    return "Kabara active";
  }

  @GET
  @Path("{dataSet}")
  @Produces(MediaType.APPLICATION_JSON)
  @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
  public Response getProcessStatus(@PathParam("dataSet") String dataSet) {
    try {
      final Optional<DataSetStatus> status = dataSetStatusManager.getStatus(dataSet);
      if (status.isPresent()) {
        return Response.ok(status).build();
      } else {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    } catch (IOException e) {
      LOG.error("Get process status failed.", e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path("{dataSet}")
  @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
  public Response setDatasetProperties(@PathParam("dataSet") String dataSet, @FormParam("graphUri") String graphUri,
                                       @FormParam("autoSync") boolean autoSync) {
    try {
      final DataSetStatus status = dataSetStatusManager.getStatusOrCreate(dataSet);
      status.updateGraphUri(graphUri);
      status.updateAutoSync(autoSync);
      dataSetStatusManager.updateStatus(dataSet, status);
      return created(fromUri(publicUrl).path(encode(dataSet, StandardCharsets.UTF_8)).build()).build();
    } catch (IOException e) {
      LOG.error("Post dataset properties failed.", e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path("{dataSet}/sync")
  public Response syncDataSet(@PathParam("dataSet") String dataSet) {
    try {
      executor.submit(datasetRunnableFactory.createRunnable(dataSet));
      return created(fromUri(publicUrl).path(encode(dataSet, StandardCharsets.UTF_8)).build()).build();
    } catch (IOException e) {
      LOG.error("Post process failed.", e);
      return Response.serverError().build();
    }
  }
}
