package nl.knaw.huc.di.kabara.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SyncRequest {
  @JsonProperty
  private String dataSet;
  private String tripleStore;

  public String getDataSet() {
    return dataSet;
  }

  public void setDataSet(String dataSet) {
    this.dataSet = dataSet;
  }

  public String getTripleStore() {
    return tripleStore;
  }

  public void setTripleStore(String tripleStore) {
    this.tripleStore = tripleStore;
  }
}
