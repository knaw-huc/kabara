package nl.knaw.huc.di.kabara.sync.graphdb_timbuctoo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import nl.knaw.huc.di.kabara.endpoints.GraphDB;
import nl.knaw.huc.di.kabara.endpoints.TimbuctooEndpoint;
import nl.knaw.huc.di.kabara.sync.Sync;
import nl.knaw.huc.di.kabara.sync.SyncManager;
import nl.knaw.huc.di.kabara.sync.SyncRunnable;

import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GraphDBTimbuctooSync extends Sync {
  @JsonProperty
  private GraphDB graphDB;

  @JsonProperty
  private TimbuctooEndpoint timbuctooEndpoint;

  @JsonProperty
  private String userId;

  @JsonProperty
  private String datasetName;

  @JsonProperty
  private String sparqlName;

  @JsonProperty
  private String authorization;

  @JsonCreator
  public GraphDBTimbuctooSync(@JsonProperty("graphDB") GraphDB graphDB,
                              @JsonProperty("timbuctooEndpoint") TimbuctooEndpoint timbuctooEndpoint,
                              @JsonProperty("userId") String userId, @JsonProperty("datasetName") String datasetName,
                              @JsonProperty("sparqlName") String sparqlName,
                              @JsonProperty("authorization") String authorization,
                              @JsonProperty("latestSync") Date latestSync, @JsonProperty("lastUpdate") Date lastUpdate,
                              @JsonProperty("autoSync") boolean autoSync,
                              @JsonProperty("status") Map<Date, String> status) {
    super(latestSync, lastUpdate, autoSync, status);
    this.graphDB = graphDB;
    this.timbuctooEndpoint = timbuctooEndpoint;
    this.userId = userId;
    this.datasetName = datasetName;
    this.sparqlName = sparqlName;
    this.authorization = authorization;
  }

  public static GraphDBTimbuctooSync create(GraphDB graphDB, TimbuctooEndpoint endpoint,
                                            String userId, String datasetName, String sparqlName) {
    return new GraphDBTimbuctooSync(graphDB, endpoint, userId, datasetName, sparqlName,
        null, null, null, true, new HashMap<>());
  }

  public GraphDB getGraphDB() {
    return graphDB;
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

  public String getSparqlName() {
    return sparqlName;
  }

  public String getAuthorization() {
    return authorization;
  }

  public void updateAuthorization(String authorization) {
    this.authorization = authorization;
  }

  @Override
  @JsonIgnore
  public Path getFile(Path path) {
    String fileName = String.format("graphdb_timbuctoo__%s__%s__%s__%s__%s.json",
        graphDB.getId(), timbuctooEndpoint.getId(), userId, datasetName, sparqlName);
    return path.resolve(fileName);
  }

  @Override
  public SyncRunnable<?> createRunnable(SyncManager syncManager) {
    return new GraphDBTimbuctooRunnable(syncManager, new Date(), this);
  }
}
