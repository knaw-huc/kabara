package nl.knaw.huc.di.kabara.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DataSetStatus {
  @JsonProperty
  private Date latestSync;

  @JsonProperty
  private Date lastUpdate;

  @JsonProperty
  private boolean autoSync;

  @JsonProperty
  private String graphUri;

  @JsonProperty
  private final Map<Date, String> imports;

  @JsonCreator
  public DataSetStatus(@JsonProperty("latestSync") Date latestSync, @JsonProperty("lastUpdate") Date lastUpdate,
                       @JsonProperty("autoSync") boolean autoSync, @JsonProperty("graphUri") String graphUri,
                       @JsonProperty("imports") Map<Date, String> imports) {
    this.latestSync = latestSync;
    this.lastUpdate = lastUpdate;
    this.autoSync = autoSync;
    this.graphUri = graphUri;
    this.imports = imports;
  }

  public static DataSetStatus createNewDataSetStatus() {
    return new DataSetStatus(null, null, true, null, new HashMap<>());
  }

  public void updateStatus(Date syncDate, String update) {
    imports.put(syncDate, update);
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

  public String getGraphUri() {
    return graphUri;
  }

  public void updateGraphUri(String graphUri) {
    this.graphUri = graphUri;
  }
}
