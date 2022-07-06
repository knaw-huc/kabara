package nl.knaw.huc.di.kabara.util;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(HttpClientBuilder.class);

  public static CloseableHttpClient create(String url) {
    return create(url, null, null);
  }

  public static CloseableHttpClient create(String url, String user, String password) {
    HttpHost target = HttpHost.create(url);

    CredentialsProvider credsProvider = null;
    if (user != null && password != null) {
      credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(
          new AuthScope(target.getHostName(), target.getPort()),
          new UsernamePasswordCredentials(user, password)
      );
    }

    return HttpClients
        .custom()
        .setDefaultCredentialsProvider(credsProvider)
        .setRetryHandler((exception, executionCount, context) -> {
          if (executionCount > 10) {
            LOG.warn("Maximum number of tries reached");
            return false;
          }

          LOG.warn("No response on try " + executionCount);
          try {
            Thread.sleep(60000L * executionCount); // A minute * execution count
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }

          return true;
        })
        .build();
  }
}
