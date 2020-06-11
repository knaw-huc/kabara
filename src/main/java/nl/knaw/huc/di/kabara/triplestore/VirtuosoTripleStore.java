package nl.knaw.huc.di.kabara.triplestore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

public class VirtuosoTripleStore implements TripleStore {
  private static final Logger LOG = LoggerFactory.getLogger(VirtuosoTripleStore.class);

  private final String url;
  private final String user;
  private final String password;
  private final String sparQlAuthPath;

  private CloseableHttpClient httpClient;
  private URI sparqlEndPoint;

  @JsonCreator
  public VirtuosoTripleStore(
      @JsonProperty("url") String url,
      @JsonProperty("user") String user,
      @JsonProperty("password") String password,
      @JsonProperty("sparQlAuthPath") String sparQlAuthPath) {
    this.url = url;
    this.user = user;
    this.password = password;
    this.sparQlAuthPath = sparQlAuthPath;
  }

  @Override
  public void sendSparQlUpdate(String sparQl) throws IOException {
    HttpPost httppost = new HttpPost(sparqlEndPoint);

    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.addTextBody("format", "application/sparql-results+xml");
    builder.addTextBody("query", sparQl);

    HttpEntity entity = builder.build();
    httppost.setEntity(entity);

    try (CloseableHttpResponse response = httpClient.execute(httppost)) {
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
  public void start() {
    sparqlEndPoint = UriBuilder.fromUri(url).path(sparQlAuthPath).build();
    HttpHost target = HttpHost.create(url);

    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
        new AuthScope(target.getHostName(), target.getPort()),
        new UsernamePasswordCredentials(user, password)
    );

    httpClient = HttpClients
        .custom()
        .setDefaultCredentialsProvider(credsProvider)
        .setRetryHandler((exception, executionCount, context) -> {
          if (executionCount > 3) {
            LOG.warn("Maximum number of tries reached for Virtuoso sync");
            return false;
          }

          LOG.warn("No response from Virtuoso on try " + executionCount);
          try {
            Thread.sleep(30000 * executionCount); // Half a minute * execution count
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
