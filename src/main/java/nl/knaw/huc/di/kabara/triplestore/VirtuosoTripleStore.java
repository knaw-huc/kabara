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

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

// Class is used via reflection
public class VirtuosoTripleStore implements TripleStore {

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
      @JsonProperty("sparQlAuthPath") String sparQlAuthPath
  ) {
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
        System.err.println("----------------------------------------");
        System.err.println("target: " + sparqlEndPoint);
        System.err.println("" + response.getStatusLine());
        System.err.println();
        System.err.println(EntityUtils.toString(response.getEntity()));
      }
    }
  }

  @Override
  public void start() throws Exception {
    sparqlEndPoint = UriBuilder.fromUri(url).path(sparQlAuthPath).build();

    HttpHost target = HttpHost.create(url);
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
        new AuthScope(target.getHostName(), target.getPort()),
        new UsernamePasswordCredentials(user, password));

    httpClient = HttpClients.custom()
                            .setDefaultCredentialsProvider(credsProvider)
                            .build();
  }

  @Override
  public void stop() throws Exception {
    httpClient.close();
  }
}
