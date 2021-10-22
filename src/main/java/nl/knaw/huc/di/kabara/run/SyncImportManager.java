package nl.knaw.huc.di.kabara.run;

import nl.knaw.huc.di.kabara.dataset.DatasetManager;
import nl.knaw.huc.di.kabara.rdfprocessing.RdfProcessor;
import nl.knaw.huc.di.kabara.rdfprocessing.parsers.NquadUdRdfHandler;
import nl.knaw.huc.di.kabara.dataset.Dataset;
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
  private final Dataset dataset;
  private final DatasetManager datasetManager;
  private final RdfProcessor rdfProcessor;

  private int counter = 0;

  public SyncImportManager(Dataset dataset, DatasetManager datasetManager, RdfProcessor rdfProcessor) {
    this.dataset = dataset;
    this.datasetManager = datasetManager;
    this.rdfProcessor = rdfProcessor;
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
        rdfParser.setRDFHandler(new NquadUdRdfHandler(rdfProcessor, dataset.getGraphUri()));
        rdfParser.parse(in, url);
      }

      dataset.updateLastUpdate(dateTime);
      datasetManager.updateDataset(dataset);

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
