package nl.knaw.huc.di.kabara;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import nl.knaw.huc.di.kabara.health.KabaraHealthCheck;
import nl.knaw.huc.di.kabara.resources.KabaraResource;
import nl.knaw.huc.di.kabara.triplestore.TripleStore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class Kabara extends Application<KabaraConfiguration> {

  static {
    try {
      SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, new TrustManager[]{new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      }
      }, null);
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new RuntimeException(e);
    }
  }

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
    int numThreads = Math.max(Runtime.getRuntime().availableProcessors() - 2, 2);
    final KabaraResource resource = new KabaraResource(
        configuration.getTemplate(),
        configuration.getConfigFileName(),
        environment.lifecycle().executorService("kabara").maxThreads(numThreads).build(),
        new RunKabara(configuration.getConfigFileName(), tripleStore)
    );
    final KabaraHealthCheck healthCheck =
        new KabaraHealthCheck();
    environment.healthChecks().register("template", healthCheck);
    // virtuoso afhankelijkheid !!!
    environment.jersey().register(resource);
  }

}
