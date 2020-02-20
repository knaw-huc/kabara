package nl.knaw.huc.di.kabara;

import nl.knaw.huc.di.kabara.rdfprocessing.RdfProcessingFailedException;
import nl.knaw.huc.di.kabara.rdfprocessing.VirtuosoRdfProcessor;
import nl.knaw.huc.di.kabara.rdfprocessing.rdf4j.Rdf4jIoFactory;
import nl.knaw.huc.di.kabara.rdfprocessing.rdf4j.Rdf4jRdfParser;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ImportStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.Future;

public class VirtuosoImportManager implements nl.knaw.huygens.timbuctoo.remote.rs.download.ImportManager {
  public static final Logger LOG = LoggerFactory.getLogger(VirtuosoImportManager.class);
  private final HttpHost target;
  private final Rdf4jRdfParser rdf4jRdfParser;
  private int counter;
  private String sparqlUri;
  private CloseableHttpClient httpClient;
  private VirtuosoRdfProcessor rdfProcessor;

  public VirtuosoImportManager(HttpHost target, CredentialsProvider credsProvider, String endpoint) {
    this.target = target;
    httpClient = HttpClients.custom()
                            .setDefaultCredentialsProvider(credsProvider)
                            .build();
    this.sparqlUri = endpoint;
    rdf4jRdfParser = new Rdf4jIoFactory().makeRdfParser();
    rdfProcessor = new VirtuosoRdfProcessor(sparql -> this.sendToSparQl(sparql, target, sparqlUri, httpClient));
    counter = 0;
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

    try {
      rdf4jRdfParser.importRdf(rdfInputStream, baseUri, defaultGraph, rdfProcessor, mediaType);
    } catch (RdfProcessingFailedException e) {
      e.printStackTrace();
    }

    return null;
  }

  public void createDb(String sparQlMutation) throws IOException {
    sendToSparQl(sparQlMutation, target, sparqlUri, httpClient);
  }


  private void sendToSparQl(String sparQlMutation, HttpHost target, String uri, CloseableHttpClient httpClient)
      throws IOException {
    counter++;
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
    final ContentType contentType = ContentType.create("text/plain", "UTF-8");

    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.addTextBody("format", "application/sparql-results+xml");
    builder.addTextBody("query", sparQlMutation);
    HttpEntity entity = builder.build();
    httppost.setEntity(entity);

    try (CloseableHttpResponse response = httpClient.execute(target, httppost, localContext)) {
      if (response.getStatusLine().getStatusCode() != 200) {
        System.err.println("----------------------------------------");
        System.err.println("target: " + target.toURI());
        System.err.println("" + response.getStatusLine());
        System.err.println();
        System.err.println(EntityUtils.toString(response.getEntity()));
      }
    }
    if (counter % 10000 == 0) {
      LOG.info("Triples imported: {}", counter);
    }
  }

  @Override
  public void addFile(InputStream inputStream, String fileName, MediaType mediaType) {
    System.out.println("addFile (does nothing!");
  }

}
