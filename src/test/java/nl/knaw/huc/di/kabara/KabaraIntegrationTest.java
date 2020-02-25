package nl.knaw.huc.di.kabara;

import com.fasterxml.jackson.databind.ObjectReader;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.Client;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DropwizardExtensionsSupport.class)
public class KabaraIntegrationTest {
  private static final DropwizardAppExtension<KabaraConfiguration> DROPWIZARD = new DropwizardAppExtension<>(
      Kabara.class, "kabara.yml",
      // Add random ports to make sure they do not clash with the default Kabara ports
      ConfigOverride.config("server.applicationConnectors[0].port", "" + ((int) (Math.random() * 10000))),
      ConfigOverride.config("server.adminConnectors[0].port", "" + ((int) (Math.random() * 10000)))
  );


  @Test
  void runServerTest() throws Exception {
    final Client client = new JerseyClientBuilder().build();
    final ObjectReader reader = DROPWIZARD.getObjectMapper().reader();

    String kabaraRunning = reader.readTree(
        client.target(String.format("http://localhost:%d/kabara", DROPWIZARD.getLocalPort()))
              .request()
              .get(String.class)
    ).get("content").asText();

    assertThat(kabaraRunning).isEqualTo("Kabara active");
  }
}
