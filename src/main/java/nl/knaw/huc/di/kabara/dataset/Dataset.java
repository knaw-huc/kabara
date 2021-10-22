package nl.knaw.huc.di.kabara.dataset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Dataset {
  @JsonProperty
  private TimbuctooEndpoint timbuctooEndpoint;

  @JsonProperty
  private String userId;

  @JsonProperty
  private String datasetName;

  @JsonProperty
  private Date latestSync;

  @JsonProperty
  private Date lastUpdate;

  @JsonProperty
  private boolean autoSync;

  @JsonProperty
  private String graphUri;

  @JsonProperty
  private final Map<Date, String> status;

  @JsonCreator
  public Dataset(@JsonProperty("timbuctooEndpoint") TimbuctooEndpoint timbuctooEndpoint,
                 @JsonProperty("userId") String userId, @JsonProperty("datasetName") String datasetName,
                 @JsonProperty("latestSync") Date latestSync, @JsonProperty("lastUpdate") Date lastUpdate,
                 @JsonProperty("autoSync") boolean autoSync, @JsonProperty("graphUri") String graphUri,
                 @JsonProperty("status") Map<Date, String> status) {
    this.timbuctooEndpoint = timbuctooEndpoint;
    this.userId = userId;
    this.datasetName = datasetName;
    this.latestSync = latestSync;
    this.lastUpdate = lastUpdate;
    this.autoSync = autoSync;
    this.graphUri = graphUri;
    this.status = status;
  }

  public static Dataset createNewDataSetStatus(TimbuctooEndpoint endpoint, String userId, String datasetName) {
    return new Dataset(endpoint, userId, datasetName,
        null, null, true, null, new HashMap<>());
  }

  public TimbuctooEndpoint getTimbuctooEndpoint() {
    return timbuctooEndpoint;
  }

  public String getUserId() {
    return userId;
  }

  public String getDatasetName() {
    return datasetName;
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

  public String getGraphUri() {
    return graphUri;
  }

  public void updateGraphUri(String graphUri) {
    this.graphUri = graphUri;
  }

  @JsonIgnore
  public String getCapabilityListUrl() {
    return String.format("%s/v5/resourcesync/%s/%s/capabilitylist.xml",
        timbuctooEndpoint.getUrl(), userId, datasetName);
  }
}
