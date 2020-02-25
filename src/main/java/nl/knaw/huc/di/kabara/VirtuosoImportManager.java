package nl.knaw.huc.di.kabara;

import nl.knaw.huc.di.kabara.rdfprocessing.RdfProcessingFailedException;
import nl.knaw.huc.di.kabara.rdfprocessing.VirtuosoRdfProcessor;
import nl.knaw.huc.di.kabara.rdfprocessing.rdf4j.Rdf4jIoFactory;
import nl.knaw.huc.di.kabara.rdfprocessing.rdf4j.Rdf4jRdfParser;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ImportStatus;
import org.apache.http.HttpEntity;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
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
  private final Rdf4jRdfParser rdf4jRdfParser;
  private String sparqlUri;
  private CloseableHttpClient httpClient;
  private VirtuosoRdfProcessor rdfProcessor;

  public VirtuosoImportManager(CredentialsProvider credsProvider, String sparQlEndpoint) {
    httpClient = HttpClients.custom()
                            .setDefaultCredentialsProvider(credsProvider)
                            .build();
    this.sparqlUri = sparQlEndpoint;
    rdf4jRdfParser = new Rdf4jIoFactory().makeRdfParser();
    rdfProcessor = new VirtuosoRdfProcessor(sparql -> this.sendToSparQl(sparql, sparqlUri, httpClient));
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
    sendToSparQl(sparQlMutation, sparqlUri, httpClient);
  }

  private void sendToSparQl(String sparQlMutation, String uri, CloseableHttpClient httpClient)
      throws IOException {
    HttpPost httppost = new HttpPost(uri);

    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.addTextBody("format", "application/sparql-results+xml");
    builder.addTextBody("query", sparQlMutation);
    HttpEntity entity = builder.build();
    httppost.setEntity(entity);


    try (CloseableHttpResponse response = httpClient.execute(httppost)) {
      if (response.getStatusLine().getStatusCode() != 200) {
        System.err.println("----------------------------------------");
        System.err.println("target: " + uri);
        System.err.println("" + response.getStatusLine());
        System.err.println();
        System.err.println(EntityUtils.toString(response.getEntity()));
      }
    }
  }

  @Override
  public void addFile(InputStream inputStream, String fileName, MediaType mediaType) {
    System.out.println("addFile (does nothing!");
  }

}
