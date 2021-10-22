package nl.knaw.huc.di.kabara.run;

import nl.knaw.huc.di.kabara.dataset.Dataset;
import nl.knaw.huc.di.kabara.dataset.DatasetManager;
import nl.knaw.huc.di.kabara.triplestore.TripleStore;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ResourceSyncFileLoader;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ResourceSyncImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;

public class DatasetRunnable implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(DatasetRunnable.class);

  private final TripleStore tripleStore;
  private final DatasetManager datasetManager;
  private final int timeout;
  private final Date currentSync;
  private Dataset dataset;

  public DatasetRunnable(TripleStore tripleStore, DatasetManager datasetManager, int timeout,
                         Date currentSync, Dataset dataset) {
    this.tripleStore = tripleStore;
    this.datasetManager = datasetManager;
    this.timeout = timeout;
    this.currentSync = currentSync;
    this.dataset = dataset;
  }

  @Override
  public void run() {
    try {
      LOG.info(String.format("dataset: %s / %s / %s",
          dataset.getTimbuctooEndpoint().getId(), dataset.getUserId(), dataset.getDatasetName()));

      dataset = datasetManager.reloadDataset(dataset);
      Date lastUpdate = dataset.getLastUpdate();
      dataset.updateLatestSync(currentSync);

      ResourceSyncImport rsi = new ResourceSyncImport(new ResourceSyncFileLoader(timeout), true);
      SyncImportManager im =
          new SyncImportManager(dataset, datasetManager, tripleStore.createRdfProcessor(this::updateCurrentSyncStatus));

      updateCurrentSyncStatus("Start import");
      rsi.filterAndImport(dataset.getCapabilityListUrl(), null, null, lastUpdate, im);

      if (im.getImportCount() > 0) {
        updateCurrentSyncStatus(String.format("Import succeeded (Files imported: %s)", im.getImportCount()));
      } else {
        updateCurrentSyncStatus("Nothing to do");
      }
    } catch (Exception e) {
      try {
        LOG.error(e.getMessage(), e);
        updateCurrentSyncStatus("Error: " + e.getMessage());
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }

  private void updateCurrentSyncStatus(String status) throws IOException {
    dataset.updateStatus(currentSync, status);
    datasetManager.updateDataset(dataset);
  }
}
