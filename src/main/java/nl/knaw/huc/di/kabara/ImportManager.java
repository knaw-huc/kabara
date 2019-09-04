package nl.knaw.huc.di.kabara;

import nl.knaw.huygens.timbuctoo.remote.rs.download.ImportStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportManager implements nl.knaw.huygens.timbuctoo.remote.rs.download.ImportManager {
  private final HttpHost target;
  private final CredentialsProvider credsProvider;
  public static final Pattern P = Pattern.compile("^([+|-])(<[^>]+>) (<[^>]+>)(.*) (<[^>]+>)$");
  private String sparqlUri;

  public ImportManager(HttpHost target, CredentialsProvider credsProvider, String endpoint) {
    this.target = target;
    this.credsProvider = credsProvider;
    this.sparqlUri = endpoint;
  }

  @Override
  public boolean isRdfTypeSupported(MediaType mediaType) {
    System.out.println(mediaType + " is Rdf Type Supported");
    return true;
  }

  @Override
  public Future<ImportStatus> addLog(String baseUri, String defaultGraph, String fileName,
                                     InputStream rdfInputStream,
                                     Optional<Charset> charset, MediaType mediaType) {
    BufferedReader in = new BufferedReader(new InputStreamReader(rdfInputStream));
    try {
      String line = in.readLine();
      while (line != null) { // && (teller++ < 20)) {
        for (String part : line.split(" \\.")) {
          Matcher matcher = P.matcher(part);
          String subject = "";
          String predicate = "";
          String object = "";
          String context = "";
          boolean add = false;
          boolean remove = false;
          if (matcher.matches()) {
            add = matcher.group(1).equals("+");
            remove = matcher.group(1).equals("-");
            subject = matcher.group(2);
            predicate = matcher.group(3);
            object = matcher.group(4).trim();
            context = matcher.group(5);
            String sparQlMutation = buildSparQlMutation(context, subject, predicate, object, add, remove);
            sendToSparQl(sparQlMutation, credsProvider, target, sparqlUri);
          } else {
            System.out.println("no match");
          }
        }
        line = in.readLine();
      }
      in.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return null;
  }

  public void createDb(String sparQlMutation) throws IOException {
    sendToSparQl(sparQlMutation, credsProvider, target, sparqlUri);
  }

  private String buildSparQlMutation(String context, String subject, String predicate, String object, boolean add,
                                     boolean remove) {
    String result = "";
    if (add) {
      result = "INSERT";
    } else if (remove) {
      result = "DELETE";
    }
    result += " DATA\n" +
        "{\n" +
        "    GRAPH " + context + " {\n" +
        "            " + subject + "\n" +
        "            " + predicate + "\n" +
        "            " + object + " .\n" +
        "    }\n" +
        "}";
    return result;
  }

  private void sendToSparQl(String sparQlMutation, CredentialsProvider credsProvider, HttpHost target, String uri)
      throws IOException {
    CloseableHttpClient httpclient = HttpClients.custom()
                                                .setDefaultCredentialsProvider(credsProvider)
                                                .build();
    try {
      // Create AuthCache instance
      AuthCache authCache = new BasicAuthCache();
      // Generate DIGEST scheme object, initialize it and add it to the local
      // auth cache
      DigestScheme digestAuth = new DigestScheme();
      // Suppose we already know the realm name
      digestAuth.overrideParamter("realm", "SPARQL");
      // Suppose we already know the expected nonce value
      digestAuth.overrideParamter("nonce", "whatever");
      authCache.put(target, digestAuth);

      // Add AuthCache to the execution context
      HttpClientContext localContext = HttpClientContext.create();
      localContext.setAuthCache(authCache);
      HttpPost httppost = new HttpPost(uri);
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.addTextBody("format", "application/sparql-results+xml");
      builder.addTextBody("query", sparQlMutation);
      HttpEntity entity = builder.build();
      httppost.setEntity(entity);
      // System.out.println("Executing request " + httppost.getRequestLine() + " to target:\n  " + target);
      // System.out.println("entity: " + EntityUtils.toString(httppost.getEntity()));

      CloseableHttpResponse response = httpclient.execute(target, httppost, localContext);
      try {
        if (response.getStatusLine().getStatusCode() != 200) {
          System.err.println("----------------------------------------");
          System.err.println("" + response.getStatusLine());
          System.err.println();
          System.err.println(EntityUtils.toString(response.getEntity()));
        }
      } finally {
        response.close();
      }
    } finally {
      httpclient.close();
    }
  }

  @Override
  public void addFile(InputStream inputStream, String fileName, MediaType mediaType) {
    System.out.println("addFile");

  }

}
