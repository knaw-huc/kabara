package nl.knaw.huc.di.kabara.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SyncRequest {
  @JsonProperty
  private String dataSet;

  public String getDataSet() {
    return dataSet;
  }

  public void setDataSet(String dataSet) {
    this.dataSet = dataSet;
  }
}
