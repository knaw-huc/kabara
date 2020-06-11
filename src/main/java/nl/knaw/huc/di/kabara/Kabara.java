package nl.knaw.huc.di.kabara;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import nl.knaw.huc.di.kabara.health.KabaraHealthCheck;
import nl.knaw.huc.di.kabara.resources.KabaraResource;
import nl.knaw.huc.di.kabara.status.DataSetStatusManager;
import nl.knaw.huc.di.kabara.triplestore.TripleStore;

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
    // Make configuration properties overridable with environment variables
    // see: https://www.dropwizard.io/en/stable/manual/core.html#environment-variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(),
        new EnvironmentVariableSubstitutor(false)
    ));
  }

  @Override
  public void run(KabaraConfiguration configuration, Environment environment) throws Exception {
    final TripleStore tripleStore = configuration.getTripleStore();
    environment.lifecycle().manage(tripleStore);

    final int numThreads = Math.max(Runtime.getRuntime().availableProcessors() - 2, 2);
    final ExecutorService kabaraExecutorService =
        environment.lifecycle().executorService("kabara").maxThreads(numThreads).build();

    final KabaraHealthCheck healthCheck = new KabaraHealthCheck();
    environment.healthChecks().register("template", healthCheck);

    final DataSetStatusManager dataSetStatusManager = configuration.getDataSetStatusManager();
    final RunKabara runKabara =
        new RunKabara(tripleStore, configuration.getResourcesyncTimeout(), dataSetStatusManager);

    final KabaraResource resource = new KabaraResource(
        kabaraExecutorService, runKabara, dataSetStatusManager, configuration.getPublicUrl());
    environment.jersey().register(resource);
  }
}
