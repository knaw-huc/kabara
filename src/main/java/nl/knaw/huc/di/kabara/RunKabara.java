package nl.knaw.huc.di.kabara;

import nl.knaw.huc.di.kabara.status.DataSetStatus;
import nl.knaw.huc.di.kabara.status.DataSetStatusManager;
import nl.knaw.huc.di.kabara.status.DataSetStatusUpdater;
import nl.knaw.huc.di.kabara.triplestore.TripleStore;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ResourceSyncFileLoader;
import nl.knaw.huygens.timbuctoo.remote.rs.download.ResourceSyncImport;
import nl.knaw.huygens.timbuctoo.remote.rs.download.exceptions.CantRetrieveFileException;
import nl.knaw.huygens.timbuctoo.remote.rs.exceptions.CantDetermineDataSetException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;

public class RunKabara {
  private static final Logger LOG = LoggerFactory.getLogger(RunKabara.class);

  private final int timeout;
  private final DataSetStatusManager dataSetStatusManager;
  private final TripleStore tripleStore;

  public RunKabara(TripleStore tripleStore, int resourcesyncTimeout, DataSetStatusManager dataSetStatusManager) {
    this.tripleStore = tripleStore;
    this.timeout = resourcesyncTimeout;
    this.dataSetStatusManager = dataSetStatusManager;
  }

  public ResourceSyncImport.ResourceSyncReport start(String dataset) {
    LOG.info("dataset: " + dataset);

    Date currentSync = new Date();
    DataSetStatusUpdater dataSetStatusUpdater = null;
    ResourceSyncImport.ResourceSyncReport resultRsi = null;

    try {
      DataSetStatus dataSetStatus = dataSetStatusManager.getStatusOrCreate(dataset);

      dataSetStatusUpdater = update -> {
        dataSetStatus.updateStatus(currentSync, update);
        dataSetStatusManager.updateStatus(dataset, dataSetStatus);
      };

      boolean isUpdate = dataSetStatus.isUpdate();
      Date lastSync = dataSetStatus.getLatestSync();
      dataSetStatus.updateLatestSync(currentSync);

      CloseableHttpClient httpclient = HttpClients.createMinimal();
      VirtuosoImportManager im = new VirtuosoImportManager(tripleStore, dataSetStatusUpdater);

      ResourceSyncImport rsi = new ResourceSyncImport(new ResourceSyncFileLoader(httpclient, timeout), true);
      dataSetStatusUpdater.updateStatus("Start import");

      resultRsi = rsi.filterAndImport(dataset, null, isUpdate, "", im, lastSync, dataset, dataset);

      dataSetStatusUpdater.updateStatus("Files imported: " + resultRsi.importedFiles);
      dataSetStatusUpdater.updateStatus("Files ignored: " + resultRsi.ignoredFiles);
      dataSetStatusUpdater.updateStatus("Import succeeded");
    } catch (CantRetrieveFileException | CantDetermineDataSetException | IOException e) {
      try {
        if (dataSetStatusUpdater != null) {
          dataSetStatusUpdater.updateStatus(String.format("can't determine dataset: %s", dataset));
          dataSetStatusUpdater.updateStatus(e.getLocalizedMessage());
          dataSetStatusUpdater.updateStatus("Kabara can't run");
        }

        e.printStackTrace();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }

    return resultRsi;
  }
}
