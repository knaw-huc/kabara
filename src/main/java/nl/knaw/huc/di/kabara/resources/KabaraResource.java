package nl.knaw.huc.di.kabara.resources;

import com.codahale.metrics.annotation.Timed;
import net.sf.saxon.s9api.SaxonApiException;
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
import java.util.concurrent.atomic.AtomicLong;


@Path("/kabara")
@Produces(MediaType.APPLICATION_JSON)
public class KabaraResource {
  private final String template;
  private final String configFileName;
  private final AtomicLong counter;
  private final RunKabara runKabara;

  public KabaraResource(String template, String configFileName) throws SaxonApiException {
    this.template = template;
    this.configFileName = configFileName;
    this.counter = new AtomicLong();
    runKabara = new RunKabara(configFileName);
  }

  @GET
  @Timed
  public Saying sayHello() {
    return new Saying(counter.incrementAndGet(), "Kabara active");
  }

  @POST
  public Response syncDataSet(SyncRequest request) {
    LoggerFactory.getLogger(KabaraResource.class).info("dataset, {}" , request.getDataSet());
    try {
      runKabara.start(request.getDataSet());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return Response.ok().build();
  }
}
