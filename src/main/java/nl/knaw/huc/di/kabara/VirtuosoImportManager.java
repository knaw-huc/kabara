package nl.knaw.huc.di.kabara;

import nl.knaw.huc.di.kabara.rdfprocessing.RdfProcessingFailedException;
import nl.knaw.huc.di.kabara.rdfprocessing.VirtuosoRdfProcessor;
import nl.knaw.huc.di.kabara.rdfprocessing.rdf4j.Rdf4jIoFactory;
import nl.knaw.huc.di.kabara.rdfprocessing.rdf4j.Rdf4jRdfParser;
import nl.knaw.huc.di.kabara.status.DataSetStatusUpdater;
import nl.knaw.huc.di.kabara.triplestore.TripleStore;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ImportStatus;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ImportManager;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class VirtuosoImportManager implements ImportManager {
  private final Rdf4jRdfParser rdf4jRdfParser;
  private final VirtuosoRdfProcessor rdfProcessor;

  public VirtuosoImportManager(TripleStore tripleStore, DataSetStatusUpdater statusUpdater) {
    rdf4jRdfParser = new Rdf4jIoFactory().makeRdfParser();
    rdfProcessor = new VirtuosoRdfProcessor(tripleStore::sendSparQlUpdate, statusUpdater);
  }

  @Override
  public boolean isRdfTypeSupported(MediaType mediaType) {
    return true;
  }

  @Override
  public Future<ImportStatus> addLog(String baseUri, String defaultGraph, String fileName, InputStream rdfInputStream,
                                     Optional<Charset> charset, MediaType mediaType) {
    try {
      File file = new File("./tmp.gz");
      if (file.exists()) {
        file.delete();
      }

      try (OutputStream out = new GZIPOutputStream(new FileOutputStream(file))) {
        IOUtils.copy(rdfInputStream, out);
      }

      try (InputStream in = new GZIPInputStream(new FileInputStream(file))) {
        rdf4jRdfParser.importRdf(in, baseUri, defaultGraph, rdfProcessor, mediaType);
      }

      file.delete();
    } catch (RdfProcessingFailedException | IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  @Override
  public void addFile(InputStream inputStream, String fileName, MediaType mediaType) {
  }
}
