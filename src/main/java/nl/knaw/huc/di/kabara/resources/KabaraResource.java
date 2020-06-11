package nl.knaw.huc.di.kabara.resources;

import com.codahale.metrics.annotation.Timed;
import nl.knaw.huc.di.kabara.RunKabara;
import nl.knaw.huc.di.kabara.api.Saying;
import nl.knaw.huc.di.kabara.api.SyncRequest;
import nl.knaw.huc.di.kabara.status.DataSetStatus;
import nl.knaw.huc.di.kabara.status.DataSetStatusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static java.net.URLEncoder.encode;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.UriBuilder.fromUri;


@Path("/kabara")
@Produces(MediaType.APPLICATION_JSON)
public class KabaraResource {
  public static final Logger LOG = LoggerFactory.getLogger(KabaraResource.class);
  private final AtomicLong counter;
  private final RunKabara runKabara;
  private final ExecutorService executor;
  private final DataSetStatusManager dataSetStatusManager;
  private final String publicUrl;

  public KabaraResource(ExecutorService executor, RunKabara runKabara,
                        DataSetStatusManager dataSetStatusManager, String publicUrl) {
    this.executor = executor;
    this.dataSetStatusManager = dataSetStatusManager;
    this.publicUrl = publicUrl;
    this.counter = new AtomicLong();
    this.runKabara = runKabara;
  }

  @GET
  @Timed
  public Saying checkActive() {
    return new Saying(counter.incrementAndGet(), "Kabara active");
  }

  @POST
  public Response syncDataSet(SyncRequest request) {
    final String dataSet = request.getDataSet();
    LOG.info("dataset, {}", dataSet);
    Callable<String> callableTask = () -> {
      try {
        runKabara.start(dataSet);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return "ok";
    };
    executor.submit(callableTask);

    try {
      return created(fromUri(publicUrl).path("kabara").path("status").path(encode(dataSet, "UTF-8")).build()).build();
    } catch (UnsupportedEncodingException e) {
      LOG.error("Failed to create status link.", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("status/{dataSet}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProcessStatus(@PathParam("dataSet") String dataSetUri) {
    try {
      final Optional<DataSetStatus> status = dataSetStatusManager.getStatus(dataSetUri);
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
}
