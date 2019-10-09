package nl.knaw.huc.di.kabara.resources;

import com.codahale.metrics.annotation.Timed;
import net.sf.saxon.s9api.SaxonApiException;
import nl.knaw.huc.di.kabara.Main;
import nl.knaw.huc.di.kabara.api.Saying;
import nl.knaw.huc.di.kabara.api.SyncRequest;
import nl.knaw.huygens.timbuctoo.remote.rs.download.exceptions.CantRetrieveFileException;
import nl.knaw.huygens.timbuctoo.remote.rs.exceptions.CantDetermineDataSetException;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicLong;


@Path("/kabara")
@Produces(MediaType.APPLICATION_JSON)
public class KabaraResource {
  private final String template;
  private final String configFileName;
  private final AtomicLong counter;

  public KabaraResource(String template, String configFileName) {
    this.template = template;
    this.configFileName = configFileName;
    this.counter = new AtomicLong();
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
      Main.start(configFileName, request.getDataSet());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return Response.ok().build();
  }
}
