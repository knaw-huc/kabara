package nl.knaw.huc.di.kabara.endpoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import nl.knaw.huc.di.kabara.processors.SparqlUpdateRdfProcessor;
import nl.knaw.huc.di.kabara.sync.SyncStatusUpdater;
import nl.knaw.huc.di.kabara.util.HttpClientBuilder;
import nl.knaw.huc.rdf4j.rio.nquadsnd.RdfProcessor;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class TripleStore {
  private static final Logger LOG = LoggerFactory.getLogger(TripleStore.class);

  private final String id;
  private final String url;
  private final String user;
  private final String password;
  private final String sparqlPath;
  private final String sparqlWritePath;

  protected final int batchSize;
  protected final CloseableHttpClient httpClient;

  @JsonCreator
  public TripleStore(
      @JsonProperty("id") String id,
      @JsonProperty("url") String url,
      @JsonProperty("user") String user,
      @JsonProperty("password") String password,
      @JsonProperty("sparqlPath") String sparqlPath,
      @JsonProperty("sparqlWritePath") String sparqlWritePath,
      @JsonProperty("batchSize") int batchSize) {
    this.id = id;
    this.url = url;
    this.user = user;
    this.password = password;
    this.sparqlPath = sparqlPath;
    this.sparqlWritePath = sparqlWritePath;
    this.batchSize = batchSize;

    httpClient = HttpClientBuilder.create(url, user, password);
  }

  public String getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }

  public String getSparqlPath() {
    return sparqlPath;
  }

  public String getSparqlWritePath() {
    return sparqlWritePath;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void sendSparqlUpdate(String sparqlName, String sparql) throws IOException {
    URI endpoint = getEndpoint(sparqlName, true);
    HttpPost httpPost = new HttpPost(endpoint);

    ContentType contentType = ContentType.create("application/sparql-update", StandardCharsets.UTF_8);
    httpPost.setEntity(new StringEntity(sparql, contentType));

    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
      if (response.getStatusLine().getStatusCode() != 200) {
        LOG.error(
            "target: {}\nstatus: {}\nmessage: {}",
            endpoint,
            response.getStatusLine(),
            EntityUtils.toString(response.getEntity())
        );
      }
    }
  }

  public void sendUrlEncodedSparqlUpdate(String sparqlName, String sparql) throws IOException {
    URI endpoint = getEndpoint(sparqlName, true);
    HttpPost httpPost = new HttpPost(endpoint);
    httpPost.setEntity(new UrlEncodedFormEntity(List.of(new BasicNameValuePair("update", sparql))));

    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
      if (response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 204) {
        LOG.error(
            "target: {}\nstatus: {}\nmessage: {}",
            endpoint,
            response.getStatusLine(),
            EntityUtils.toString(response.getEntity())
        );
      }
    }
  }

  public RdfProcessor createRdfSparqlUpdateProcessor(SyncStatusUpdater syncStatusUpdater, String sparqlName) {
    return new SparqlUpdateRdfProcessor(this, syncStatusUpdater, sparqlName, batchSize);
  }

  protected URI getEndpoint(String sparqlName, boolean writable) {
    String path = writable ? sparqlWritePath : sparqlPath;
    path = path.replace("{name}", sparqlName);
    return UriBuilder.fromUri(url).path(path).build();
  }
}
