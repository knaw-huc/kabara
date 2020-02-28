package nl.knaw.huc.di.kabara;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import nl.knaw.huc.di.kabara.status.DataSetStatusManager;
import nl.knaw.huc.di.kabara.triplestore.TripleStore;

import javax.validation.constraints.NotNull;

public class KabaraConfiguration extends Configuration {

  @JsonProperty
  @NotNull
  private DataSetStatusManager dataSetStatusManager;
  @JsonProperty
  @NotNull
  private TripleStore tripleStore;

  @JsonProperty
  @NotNull
  private int resourcesyncTimeout;

  @JsonProperty
  @NotNull
  private String publicUrl;

  public TripleStore getTripleStore() {
    return tripleStore;
  }

  public int getResourcesyncTimeout() {
    return resourcesyncTimeout;
  }

  public DataSetStatusManager getDataSetStatusManager() {
    return dataSetStatusManager;
  }

  public String getPublicUrl() {
    return this.publicUrl;
  }
}
