package nl.knaw.huc.di.kabara.resources;

import com.codahale.metrics.annotation.Timed;
import net.sf.saxon.s9api.SaxonApiException;
import nl.knaw.huc.di.kabara.RunKabara;
import nl.knaw.huc.di.kabara.api.Saying;
import nl.knaw.huc.di.kabara.api.SyncRequest;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;


@Path("/kabara")
@Produces(MediaType.APPLICATION_JSON)
public class KabaraResource {
  private final String template;
  private final String configFileName;
  private final AtomicLong counter;
  private final RunKabara runKabara;
  private final ExecutorService executor;

  public KabaraResource(String template, String configFileName, ExecutorService executor, RunKabara runKabara) {
    this.template = template;
    this.configFileName = configFileName;
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
    LoggerFactory.getLogger(KabaraResource.class).info("dataset, {}" , request.getDataSet());
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
