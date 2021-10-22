package nl.knaw.huc.di.kabara;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import nl.knaw.huc.di.kabara.dataset.DatasetManager;
import nl.knaw.huc.di.kabara.dataset.TimbuctooEndpoint;
import nl.knaw.huc.di.kabara.triplestore.TripleStore;

import javax.validation.constraints.NotNull;
import java.util.List;

public class KabaraConfiguration extends Configuration {
  @JsonProperty
  @NotNull
  private List<TimbuctooEndpoint> timbuctooEndpoints;

  @JsonProperty
  @NotNull
  private DatasetManager datasetManager;

  @JsonProperty
  @NotNull
  private TripleStore tripleStore;

  @JsonProperty
  @NotNull
  private int resourceSyncTimeout;

  @JsonProperty
  @NotNull
  private String publicUrl;

  public List<TimbuctooEndpoint> getTimbuctooEndpoints() {
    return timbuctooEndpoints;
  }

  public DatasetManager getDatasetManager() {
    return datasetManager;
  }

  public TripleStore getTripleStore() {
    return tripleStore;
  }

  public int getResourceSyncTimeout() {
    return resourceSyncTimeout;
  }

  public String getPublicUrl() {
    return this.publicUrl;
  }
}
