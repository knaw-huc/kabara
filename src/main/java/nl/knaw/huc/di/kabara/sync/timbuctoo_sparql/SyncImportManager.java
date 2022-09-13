package nl.knaw.huc.di.kabara.sync.timbuctoo_sparql;

import nl.knaw.huc.di.kabara.sync.SyncManager;
import nl.knaw.huc.rdf4j.rio.nquadsnd.NQuadsUdHandler;
import nl.knaw.huc.rdf4j.rio.nquadsnd.RdfProcessor;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ResourceSyncImport;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SyncImportManager implements ResourceSyncImport.WithFile {
  private final TimbuctooSparqlSync dataset;
  private final SyncManager syncManager;
  private final RdfProcessor rdfProcessor;

  private int counter = 0;

  public SyncImportManager(TimbuctooSparqlSync dataset, SyncManager syncManager, RdfProcessor rdfProcessor) {
    this.dataset = dataset;
    this.syncManager = syncManager;
    this.rdfProcessor = rdfProcessor;
  }

  @Override
  public Future<?> withFile(InputStream inputStream, String url, String mediaType, Date dateTime) {
    Path file = null;
    try {
      RDFFormat format = Rio.getParserFormatForMIMEType(mediaType).orElseThrow(
          () -> new UnsupportedRDFormatException(mediaType + " is not a supported rdf type.")
      );

      file = Files.createTempFile("timbuctoo_sparql", null);
      try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(file))) {
        IOUtils.copy(inputStream, out);
      }

      try (InputStream in = new GZIPInputStream(Files.newInputStream(file))) {
        RDFParser rdfParser = Rio.createParser(format);
        rdfParser.setPreserveBNodeIDs(true);
        rdfParser.setRDFHandler(new NQuadsUdHandler(rdfProcessor, dataset.getGraphUri()));
        rdfParser.parse(in, url);
      }

      dataset.updateLastUpdate(dateTime);
      syncManager.updateSync(dataset);

      counter++;
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (file != null) {
        try {
          Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
      }
    }

    return null;
  }

  public int getImportCount() {
    return counter;
  }
}
