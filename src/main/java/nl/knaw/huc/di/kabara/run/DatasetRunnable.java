package nl.knaw.huc.di.kabara.run;

import nl.knaw.huc.di.kabara.status.DataSetStatus;
import nl.knaw.huc.di.kabara.status.DataSetStatusManager;
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
  private final int timeout;
  private final DataSetStatusManager dataSetStatusManager;
  private final String dataset;
  private final Date currentSync;
  private final DataSetStatus dataSetStatus;

  public DatasetRunnable(TripleStore tripleStore, int timeout, DataSetStatusManager dataSetStatusManager,
                         String dataset, Date currentSync, DataSetStatus dataSetStatus) {
    this.tripleStore = tripleStore;
    this.timeout = timeout;
    this.dataSetStatusManager = dataSetStatusManager;
    this.dataset = dataset;
    this.currentSync = currentSync;
    this.dataSetStatus = dataSetStatus;
  }

  @Override
  public void run() {
    try {
      LOG.info("dataset: " + dataset);

      Date lastUpdate = dataSetStatus.getLastUpdate();
      dataSetStatus.updateLatestSync(currentSync);

      ResourceSyncImport rsi = new ResourceSyncImport(new ResourceSyncFileLoader(timeout), true);
      SyncImportManager im = new SyncImportManager(dataSetStatus.getGraphUri(),
          tripleStore.createRdfProcessor(this::updateCurrentSyncStatus), this::updateLastUpdate);

      updateCurrentSyncStatus("Start import");
      rsi.filterAndImport(dataset, null, null, lastUpdate, im);

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
    dataSetStatus.updateStatus(currentSync, status);
    dataSetStatusManager.updateStatus(dataset, dataSetStatus);
  }

  private void updateLastUpdate(Date dateTime) throws IOException {
    dataSetStatus.updateLastUpdate(dateTime);
    dataSetStatusManager.updateStatus(dataset, dataSetStatus);
  }
}
