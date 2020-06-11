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
    timeout = resourcesyncTimeout;
    this.dataSetStatusManager = dataSetStatusManager;
  }

  public ResourceSyncImport.ResourceSyncReport start(String dataset) throws IOException {
    LOG.info("dataset: " + dataset);

    final DataSetStatus dataSetStatus = dataSetStatusManager.getStatusOrCreate(dataset);
    final Date lastSync = dataSetStatus.getLatestSync();
    final boolean isUpdate = dataSetStatus.isUpdate();

    Date currentSync = new Date();
    dataSetStatus.updateLatestSync(currentSync);

    CloseableHttpClient httpclient = HttpClients.createMinimal();

    final DataSetStatusUpdater dataSetStatusUpdater = update -> {
      dataSetStatus.updateStatus(currentSync, update);
      dataSetStatusManager.updateStatus(dataset, dataSetStatus);
    };

    VirtuosoImportManager im = new VirtuosoImportManager(tripleStore, dataSetStatusUpdater);

    ResourceSyncImport rsi = new ResourceSyncImport(new ResourceSyncFileLoader(httpclient, timeout), true);
    dataSetStatusUpdater.updateStatus("Start import");
    ResourceSyncImport.ResourceSyncReport resultRsi = null;

    try {
      resultRsi = rsi.filterAndImport(dataset, null, isUpdate, "", im, lastSync, dataset, dataset);
    } catch (CantRetrieveFileException e) {
      // inform user
      dataSetStatusUpdater.updateStatus(String.format("can't retrieve file from dataset: %s", dataset));
      dataSetStatusUpdater.updateStatus(e.getLocalizedMessage());
      dataSetStatusUpdater.updateStatus("Kabara can't run");
      e.printStackTrace();
    } catch (CantDetermineDataSetException e) {
      // inform user
      dataSetStatusUpdater.updateStatus(String.format("can't determine dataset: %s", dataset));
      dataSetStatusUpdater.updateStatus(e.getLocalizedMessage());
      dataSetStatusUpdater.updateStatus("Kabara can't run");
      e.printStackTrace();
    }

    dataSetStatusUpdater.updateStatus("Files imported: " + resultRsi.importedFiles);
    dataSetStatusUpdater.updateStatus("Files ignored: " + resultRsi.ignoredFiles);
    dataSetStatusUpdater.updateStatus("Import succeeded");

    return resultRsi;
  }
}
