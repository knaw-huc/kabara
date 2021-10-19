package nl.knaw.huc.di.kabara;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import nl.knaw.huc.di.kabara.rdfprocessing.parsers.NquadsUdParser;
import nl.knaw.huc.di.kabara.resources.KabaraResource;
import nl.knaw.huc.di.kabara.run.DatasetRunnableFactory;
import nl.knaw.huc.di.kabara.status.DataSetStatusManager;
import nl.knaw.huc.di.kabara.triplestore.TripleStore;
import org.eclipse.rdf4j.rio.RDFParserRegistry;

import java.util.concurrent.ExecutorService;

public class Kabara extends Application<KabaraConfiguration> {
  public static void main(String[] args) throws Exception {
    RDFParserRegistry.getInstance().add(new NquadsUdParser.NquadsUdParserFactory());
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
    final TripleStore tripleStore = configuration.getTripleStore();
    environment.lifecycle().manage(tripleStore);

    final int numThreads = Math.max(Runtime.getRuntime().availableProcessors() - 2, 2);
    final ExecutorService kabaraExecutorService =
        environment.lifecycle().executorService("kabara").maxThreads(numThreads).build();

    final DataSetStatusManager dataSetStatusManager = configuration.getDataSetStatusManager();
    final DatasetRunnableFactory datasetRunnableFactory =
        new DatasetRunnableFactory(tripleStore, configuration.getResourceSyncTimeout(), dataSetStatusManager);

    final KabaraResource resource = new KabaraResource(
        kabaraExecutorService, datasetRunnableFactory, dataSetStatusManager, configuration.getPublicUrl());
    environment.jersey().register(resource);
  }
}
