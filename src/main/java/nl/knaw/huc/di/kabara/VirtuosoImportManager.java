package nl.knaw.huc.di.kabara;

import nl.knaw.huc.di.kabara.rdfprocessing.RdfProcessingFailedException;
import nl.knaw.huc.di.kabara.rdfprocessing.VirtuosoRdfProcessor;
import nl.knaw.huc.di.kabara.rdfprocessing.rdf4j.Rdf4jIoFactory;
import nl.knaw.huc.di.kabara.rdfprocessing.rdf4j.Rdf4jRdfParser;
import nl.knaw.huc.di.kabara.triplestore.TripleStore;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ImportStatus;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.Future;

public class VirtuosoImportManager implements nl.knaw.huygens.timbuctoo.remote.rs.download.ImportManager {
  private final Rdf4jRdfParser rdf4jRdfParser;
  private final TripleStore tripleStore;
  private VirtuosoRdfProcessor rdfProcessor;

  public VirtuosoImportManager(TripleStore tripleStore) {
    rdf4jRdfParser = new Rdf4jIoFactory().makeRdfParser();
    this.tripleStore = tripleStore;
    rdfProcessor = new VirtuosoRdfProcessor(this.tripleStore::sendSparQlUpdate);
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
    tripleStore.sendSparQlUpdate(sparQlMutation);
  }

  @Override
  public void addFile(InputStream inputStream, String fileName, MediaType mediaType) {
    System.out.println("addFile (does nothing!");
  }

}
