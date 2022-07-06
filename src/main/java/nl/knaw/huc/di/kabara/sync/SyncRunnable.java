package nl.knaw.huc.di.kabara.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;

public abstract class SyncRunnable<S extends Sync> implements Runnable, SyncStatusUpdater {
  private static final Logger LOG = LoggerFactory.getLogger(SyncRunnable.class);
  protected final SyncManager syncManager;
  protected final Date currentSync;
  protected S sync;

  public SyncRunnable(SyncManager syncManager, Date currentSync, S sync) {
    this.syncManager = syncManager;
    this.currentSync = currentSync;
    this.sync = sync;
  }

  @Override
  public void run() {
    try {
      LOG.info(String.format("Sync starting: %s", sync));

      sync = syncManager.reloadSync(sync);
      Date lastUpdate = sync.getLastUpdate();
      sync.updateLatestSync(currentSync);

      startSync(lastUpdate);

      LOG.info(String.format("Sync finished: %s", sync));
    } catch (Exception e) {
      try {
        LOG.error(e.getMessage(), e);
        updateStatus("Error: " + e.getMessage());
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }

  @Override
  public void updateStatus(String statusUpdate) throws IOException {
    sync.updateStatus(currentSync, statusUpdate);
    syncManager.updateSync(sync);
  }

  public abstract void startSync(Date lastUpdate) throws Exception;
}
