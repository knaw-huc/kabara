package nl.knaw.huc.di.kabara.endpoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nl.knaw.huc.di.kabara.util.HttpClientBuilder;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class TimbuctooEndpoint {
  private static final Logger LOG = LoggerFactory.getLogger(TimbuctooEndpoint.class);

  private final String id;
  private final String url;
  private final CloseableHttpClient httpClient;

  @JsonCreator
  public TimbuctooEndpoint(@JsonProperty("id") String id, @JsonProperty("url") String url) {
    this.id = id;
    this.url = url;
    httpClient = HttpClientBuilder.create(url);
  }

  public String getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public void uploadRDF(String userId, String datasetName, String authorization,
                        Path file, String contentType) throws IOException {
    URI uploadEndpoint = UriBuilder.fromUri(url).path("v5").path(userId).path(datasetName)
                                   .path("upload/rdf").queryParam("async", true).build();

    HttpEntity entity = MultipartEntityBuilder
        .create()
        .addTextBody("encoding", "UTF-8")
        .addBinaryBody("file", file.toFile(), ContentType.create(contentType, Consts.UTF_8), null)
        .build();

    HttpPost httpPost = new HttpPost(uploadEndpoint);
    httpPost.setHeader("Authorization", authorization);
    httpPost.setEntity(entity);

    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
      if (response.getStatusLine().getStatusCode() != 202) {
        LOG.error(
            "target: {}\nstatus: {}\nmessage: {}",
            uploadEndpoint,
            response.getStatusLine(),
            EntityUtils.toString(response.getEntity())
        );
      }
    }
  }
}
