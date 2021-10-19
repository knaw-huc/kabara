package nl.knaw.huc.di.kabara.run;

import nl.knaw.huc.di.kabara.rdfprocessing.RdfProcessor;
import nl.knaw.huc.di.kabara.rdfprocessing.parsers.NquadUdRdfHandler;
import nl.knaw.huc.di.kabara.status.DataSetLastUpdateWriter;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ResourceSyncImport;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SyncImportManager implements ResourceSyncImport.WithFile {
  private final String defaultGraph;
  private final RdfProcessor rdfProcessor;
  private final DataSetLastUpdateWriter dataSetLastUpdateWriter;

  private int counter = 0;

  public SyncImportManager(String defaultGraph, RdfProcessor rdfProcessor,
                           DataSetLastUpdateWriter dataSetLastUpdateWriter) {
    this.defaultGraph = defaultGraph;
    this.rdfProcessor = rdfProcessor;
    this.dataSetLastUpdateWriter = dataSetLastUpdateWriter;
  }

  @Override
  public Future<?> withFile(InputStream inputStream, String url, String mediaType, Date dateTime) {
    File file = null;
    try {
      RDFFormat format = Rio.getParserFormatForMIMEType(mediaType).orElseThrow(
          () -> new UnsupportedRDFormatException(mediaType + " is not a supported rdf type.")
      );

      file = File.createTempFile("kabara", null);
      try (OutputStream out = new GZIPOutputStream(new FileOutputStream(file))) {
        IOUtils.copy(inputStream, out);
      }

      try (InputStream in = new GZIPInputStream(new FileInputStream(file))) {
        RDFParser rdfParser = Rio.createParser(format);
        rdfParser.setPreserveBNodeIDs(true);
        rdfParser.setRDFHandler(new NquadUdRdfHandler(rdfProcessor, defaultGraph));
        rdfParser.parse(in, url);
      }

      dataSetLastUpdateWriter.updateLastUpdate(dateTime);
      counter++;
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (file != null) {
        file.delete();
      }
    }

    return null;
  }

  public int getImportCount() {
    return counter;
  }
}
