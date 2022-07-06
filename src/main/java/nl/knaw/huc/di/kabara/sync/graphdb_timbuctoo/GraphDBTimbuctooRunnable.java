package nl.knaw.huc.di.kabara.sync.graphdb_timbuctoo;

import nl.knaw.huc.di.kabara.sync.SyncManager;
import nl.knaw.huc.di.kabara.sync.SyncRunnable;
import org.knaw.huc.di.rdf4j.rio.nquadsnd.NQuadsUdParserFactory;
import org.knaw.huc.di.rdf4j.rio.nquadsnd.NQuadsUdWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import static nl.knaw.huc.di.kabara.endpoints.GraphDB.AssertedStatement;

public class GraphDBTimbuctooRunnable extends SyncRunnable<GraphDBTimbuctooSync> {
  public GraphDBTimbuctooRunnable(SyncManager syncManager, Date currentSync, GraphDBTimbuctooSync sync) {
    super(syncManager, currentSync, sync);
  }

  @Override
  public void startSync(Date lastUpdate) throws Exception {
    Path file = null;
    try {
      updateStatus("Start obtaining history from GraphDB");

      int count = 0;
      file = Files.createTempFile("graphdb_timbuctoo", null);
      try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(file))) {
        NQuadsUdWriter writer = new NQuadsUdWriter(out);
        writer.startRDF();

        for (AssertedStatement assertedSt : sync.getGraphDB()
                                                .withChanges(sync.getSparqlName(), lastUpdate, currentSync)) {
          count++;
          writer.consumeStatement(assertedSt.isAssertion(), assertedSt.statement());
        }

        writer.endRDF();
      }

      if (count > 0) {
        updateStatus("Obtained history from GraphDB; start importing to Timbuctoo");

        String contentType = NQuadsUdParserFactory.NQUADS_UD_FORMAT.getDefaultMIMEType();
        sync.getTimbuctooEndpoint().uploadRDF(sync.getUserId(), sync.getDatasetName(),
            sync.getAuthorization(), file, contentType);

        updateStatus("Updated history to Timbuctoo; remove synced history from GraphDB");
        sync.getGraphDB().deleteChanges(sync.getSparqlName(), currentSync);

        updateStatus("All done!");
      } else {
        updateStatus("No history to obtain from GraphDB");
      }
    } finally {
      if (file != null) {
        try {
          Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
      }
    }
  }
}
