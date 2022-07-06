package nl.knaw.huc.di.kabara.sync.timbuctoo_sparql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import nl.knaw.huc.di.kabara.endpoints.TimbuctooEndpoint;
import nl.knaw.huc.di.kabara.endpoints.TripleStore;
import nl.knaw.huc.di.kabara.sync.Sync;
import nl.knaw.huc.di.kabara.sync.SyncManager;
import nl.knaw.huc.di.kabara.sync.SyncRunnable;

import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TimbuctooSparqlSync extends Sync {
  @JsonProperty
  private TimbuctooEndpoint timbuctooEndpoint;

  @JsonProperty
  private TripleStore tripleStore;

  @JsonProperty
  private String userId;

  @JsonProperty
  private String datasetName;

  @JsonProperty
  private String sparqlName;

  @JsonProperty
  private String graphUri;

  @JsonCreator
  public TimbuctooSparqlSync(@JsonProperty("timbuctooEndpoint") TimbuctooEndpoint timbuctooEndpoint,
                             @JsonProperty("tripleStore") TripleStore tripleStore,
                             @JsonProperty("userId") String userId, @JsonProperty("datasetName") String datasetName,
                             @JsonProperty("sparqlName") String sparqlName,
                             @JsonProperty("latestSync") Date latestSync, @JsonProperty("lastUpdate") Date lastUpdate,
                             @JsonProperty("autoSync") boolean autoSync, @JsonProperty("graphUri") String graphUri,
                             @JsonProperty("status") Map<Date, String> status) {
    super(latestSync, lastUpdate, autoSync, status);
    this.timbuctooEndpoint = timbuctooEndpoint;
    this.tripleStore = tripleStore;
    this.userId = userId;
    this.datasetName = datasetName;
    this.sparqlName = sparqlName;
    this.graphUri = graphUri;
  }

  public static TimbuctooSparqlSync create(TimbuctooEndpoint endpoint, TripleStore tripleStore,
                                           String userId, String datasetName, String sparqlName) {
    return new TimbuctooSparqlSync(endpoint, tripleStore, userId, datasetName, sparqlName,
        null, null, true, null, new HashMap<>());
  }

  public TimbuctooEndpoint getTimbuctooEndpoint() {
    return timbuctooEndpoint;
  }

  public TripleStore getTripleStore() {
    return tripleStore;
  }

  public String getUserId() {
    return userId;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public String getSparqlName() {
    return sparqlName;
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

  @Override
  @JsonIgnore
  public Path getFile(Path path) {
    String fileName = String.format("timbuctoo_sparql__%s__%s__%s__%s__%s.json",
        timbuctooEndpoint.getId(), tripleStore.getId(), userId, datasetName, sparqlName);
    return path.resolve(fileName);
  }

  @Override
  public SyncRunnable<?> createRunnable(SyncManager syncManager) {
    return new TimbuctooSparqlRunnable(syncManager, new Date(), this);
  }
}
