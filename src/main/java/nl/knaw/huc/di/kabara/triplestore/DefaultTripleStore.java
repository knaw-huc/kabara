package nl.knaw.huc.di.kabara.triplestore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nl.knaw.huc.di.kabara.rdfprocessing.RdfProcessor;
import nl.knaw.huc.di.kabara.rdfprocessing.SparqlUpdateRdfProcessor;
import nl.knaw.huc.di.kabara.status.DataSetStatusUpdater;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DefaultTripleStore implements TripleStore {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultTripleStore.class);

  private final String url;
  private final String user;
  private final String password;
  private final String sparqlPath;
  private final int batchSize;

  private URI sparqlEndPoint;
  private CloseableHttpClient httpClient;

  @JsonCreator
  public DefaultTripleStore(
      @JsonProperty("url") String url,
      @JsonProperty("user") String user,
      @JsonProperty("password") String password,
      @JsonProperty("sparqlPath") String sparqlPath,
      @JsonProperty("batchSize") int batchSize) {
    this.url = url;
    this.user = user;
    this.password = password;
    this.sparqlPath = sparqlPath;
    this.batchSize = batchSize;
  }

  @Override
  public void sendSparqlUpdate(String sparql) throws IOException {
    HttpPost httpPost = new HttpPost(sparqlEndPoint);

    ContentType contentType = ContentType.create("application/sparql-update", StandardCharsets.UTF_8);
    httpPost.setEntity(new StringEntity(sparql, contentType));

    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
      if (response.getStatusLine().getStatusCode() != 200) {
        LOG.error(
            "target: {}\nstatus: {}\nmessage: {}",
            sparqlEndPoint,
            response.getStatusLine(),
            EntityUtils.toString(response.getEntity())
        );
      }
    }
  }

  @Override
  public RdfProcessor createRdfProcessor(DataSetStatusUpdater dataSetStatusUpdater) {
    return new SparqlUpdateRdfProcessor(this, dataSetStatusUpdater, batchSize);
  }

  @Override
  public void start() {
    sparqlEndPoint = UriBuilder.fromUri(url).path(sparqlPath).build();
    HttpHost target = HttpHost.create(url);

    CredentialsProvider credsProvider = null;
    if (user != null && password != null) {
      credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(
          new AuthScope(target.getHostName(), target.getPort()),
          new UsernamePasswordCredentials(user, password)
      );
    }

    httpClient = HttpClients
        .custom()
        .setDefaultCredentialsProvider(credsProvider)
        .setRetryHandler((exception, executionCount, context) -> {
          if (executionCount > 10) {
            LOG.warn("Maximum number of tries reached for reaching the triple store");
            return false;
          }

          LOG.warn("No response from the triple store on try " + executionCount);
          try {
            Thread.sleep(60000L * executionCount); // A minute * execution count
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }

          return true;
        })
        .build();
  }

  @Override
  public void stop() throws Exception {
    httpClient.close();
  }
}
