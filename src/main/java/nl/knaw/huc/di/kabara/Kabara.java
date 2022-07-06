package nl.knaw.huc.di.kabara;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import nl.knaw.huc.di.kabara.sync.graphdb_timbuctoo.GraphDBTimbuctooResource;
import nl.knaw.huc.di.kabara.sync.timbuctoo_sparql.TimbuctooSparqlResource;
import nl.knaw.huc.di.kabara.sync.SyncManager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.concurrent.ExecutorService;

public class Kabara extends Application<KabaraConfiguration> {
  public static void main(String[] args) throws Exception {
    new Kabara().run(args);
  }

  @Override
  public String getName() {
    return "kabara";
  }

  @Override
  public void initialize(Bootstrap<KabaraConfiguration> bootstrap) {
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(),
        new EnvironmentVariableSubstitutor(false)
    ));
  }

  @Override
  public void run(KabaraConfiguration configuration, Environment environment) {
    final int numThreads = Math.max(Runtime.getRuntime().availableProcessors() - 2, 2);
    final ExecutorService kabaraExecutorService =
        environment.lifecycle().executorService("kabara").maxThreads(numThreads).build();

    environment.jersey().register(new KabaraResource());
    environment.jersey().register(new TimbuctooSparqlResource(kabaraExecutorService, configuration));
    environment.jersey().register(new GraphDBTimbuctooResource(kabaraExecutorService, configuration));
  }

  @Path("/")
  public static final class KabaraResource {
    @GET
    public String checkActive() {
      return "Kabara active";
    }
  }
}
