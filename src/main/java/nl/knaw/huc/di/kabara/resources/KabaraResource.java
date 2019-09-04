package nl.knaw.huc.di.kabara.resources;

import com.codahale.metrics.annotation.Timed;
import nl.knaw.huc.di.kabara.api.Saying;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.atomic.AtomicLong;


@Path("/kabara")
@Produces(MediaType.APPLICATION_JSON)
public class KabaraResource {
  private final String template;
  private final AtomicLong counter;

  public KabaraResource(String template) {
    this.template = template;
    this.counter = new AtomicLong();
  }

  @GET
  @Timed
  public Saying sayHello() {
    return new Saying(counter.incrementAndGet(), "Kabara active");
  }
}
