package nl.knaw.huc.di.kabara.sync;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.util.Date;
import java.util.Map;

public abstract class Sync {
  @JsonProperty
  private Date latestSync;

  @JsonProperty
  private Date lastUpdate;

  @JsonProperty
  private boolean autoSync;

  @JsonProperty
  private final Map<Date, String> status;

  public Sync(Date latestSync, Date lastUpdate, boolean autoSync, Map<Date, String> status) {
    this.latestSync = latestSync;
    this.lastUpdate = lastUpdate;
    this.autoSync = autoSync;
    this.status = status;
  }

  public void updateStatus(Date syncDate, String update) {
    status.put(syncDate, update);
  }

  public void updateLatestSync(Date latestSync) {
    this.latestSync = latestSync;
  }

  public Date getLastUpdate() {
    return lastUpdate;
  }

  public void updateLastUpdate(Date lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  public boolean isAutoSync() {
    return autoSync;
  }

  public void updateAutoSync(boolean autoSync) {
    this.autoSync = autoSync;
  }

  public abstract Path getFile(Path path);

  public abstract SyncRunnable<?> createRunnable(SyncManager syncManager);
}
