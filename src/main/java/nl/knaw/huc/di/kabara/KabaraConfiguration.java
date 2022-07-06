package nl.knaw.huc.di.kabara;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import nl.knaw.huc.di.kabara.endpoints.GraphDB;
import nl.knaw.huc.di.kabara.endpoints.TimbuctooEndpoint;
import nl.knaw.huc.di.kabara.sync.SyncManager;
import nl.knaw.huc.di.kabara.endpoints.TripleStore;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

public class KabaraConfiguration extends Configuration {
  @JsonProperty
  @NotNull
  private List<TimbuctooEndpoint> timbuctooEndpoints;

  @JsonProperty
  @NotNull
  private List<TripleStore> tripleStores;

  @JsonProperty
  @NotNull
  private SyncManager syncManager;

  @JsonProperty
  @NotNull
  private String publicUrl;

  public List<TimbuctooEndpoint> getTimbuctooEndpoints() {
    return timbuctooEndpoints;
  }

  public List<TripleStore> getTripleStores() {
    return tripleStores;
  }

  public List<GraphDB> getGraphDBs() {
    return tripleStores.stream()
                       .filter(tripleStore -> tripleStore instanceof GraphDB)
                       .map(tripleStore -> (GraphDB) tripleStore)
                       .collect(Collectors.toList());
  }

  public SyncManager getSyncManager() {
    return syncManager;
  }

  public String getPublicUrl() {
    return this.publicUrl;
  }
}
