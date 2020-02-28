package nl.knaw.huc.di.kabara.resources;

import com.codahale.metrics.annotation.Timed;
import nl.knaw.huc.di.kabara.RunKabara;
import nl.knaw.huc.di.kabara.api.Saying;
import nl.knaw.huc.di.kabara.api.SyncRequest;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;


@Path("/kabara")
@Produces(MediaType.APPLICATION_JSON)
public class KabaraResource {
  private final AtomicLong counter;
  private final RunKabara runKabara;
  private final ExecutorService executor;

  public KabaraResource(ExecutorService executor, RunKabara runKabara) {
    this.executor = executor;
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
    LoggerFactory.getLogger(KabaraResource.class).info("dataset, {}", request.getDataSet());
    Callable<String> callableTask = new Callable<String>() {
      @Override
      public String call() throws Exception {
        try {
          runKabara.start(request.getDataSet());
        } catch (Exception e) {
          e.printStackTrace();
        }
        return "ok";
      }
    };
    executor.submit(callableTask);
    return Response.ok().build();
  }
}
