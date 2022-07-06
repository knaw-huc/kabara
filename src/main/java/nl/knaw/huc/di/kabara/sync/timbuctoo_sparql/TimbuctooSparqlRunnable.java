package nl.knaw.huc.di.kabara.sync.timbuctoo_sparql;

import nl.knaw.huc.di.kabara.sync.SyncManager;
import nl.knaw.huc.di.kabara.sync.SyncRunnable;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ResourceSyncFileLoader;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ResourceSyncImport;
import org.knaw.huc.di.rdf4j.rio.nquadsnd.RdfProcessor;

import java.util.Date;

public class TimbuctooSparqlRunnable extends SyncRunnable<TimbuctooSparqlSync> {
  public TimbuctooSparqlRunnable(SyncManager syncManager, Date currentSync, TimbuctooSparqlSync sync) {
    super(syncManager, currentSync, sync);
  }

  @Override
  public void startSync(Date lastUpdate) throws Exception {
    RdfProcessor rdfProcessor = sync.getTripleStore()
                                    .createRdfSparqlUpdateProcessor(this, sync.getSparqlName());
    ResourceSyncImport rsi = new ResourceSyncImport(new ResourceSyncFileLoader(), true);
    SyncImportManager im = new SyncImportManager(sync, syncManager, rdfProcessor);

    updateStatus("Start import");
    rsi.filterAndImport(sync.getCapabilityListUrl(), null, null, lastUpdate, im);

    if (im.getImportCount() > 0) {
      updateStatus(String.format("Import succeeded (Files imported: %s)", im.getImportCount()));
    } else {
      updateStatus("Nothing to do");
    }
  }
}
