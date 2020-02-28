package nl.knaw.huc.di.kabara.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DataSetStatus {
  @JsonProperty
  private final Map<Date, String> imports;
  @JsonProperty
  private Date latestSync;

  @JsonCreator
  public DataSetStatus(@JsonProperty("latestSync") Date latestSync,
                       @JsonProperty("imports") Map<Date, String> imports
  ) {
    this.latestSync = latestSync;
    this.imports = imports;
  }

  public static DataSetStatus createNewDataSetStatus() {
    return new DataSetStatus(new Date(), new HashMap<>());
  }

  public void updateStatus(Date syncDate, String update) {
    imports.compute(syncDate, (key, value) -> value == null ? update : value + "\n" + update);
  }

  public void updateLatestSync(Date latestSync) {
    this.latestSync = latestSync;
  }

  public Date getLatestSync() {
    return latestSync;
  }

  @JsonIgnore
  public boolean isUpdate() {
    return !imports.isEmpty();
  }
}
