package nl.knaw.huc.di.kabara.sync.timbuctoo_sparql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;
import nl.knaw.huc.di.kabara.KabaraConfiguration;
import nl.knaw.huc.di.kabara.endpoints.TimbuctooEndpoint;
import nl.knaw.huc.di.kabara.endpoints.TripleStore;
import nl.knaw.huc.di.kabara.sync.SyncResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Path("/timbuctoo/sparql/")
@Produces(MediaType.APPLICATION_JSON)
public class TimbuctooSparqlResource extends SyncResource {
  public TimbuctooSparqlResource(ExecutorService executor, KabaraConfiguration configuration) {
    super(executor, configuration, "/timbuctoo/sparql/");
  }

  @GET
  @Path("{endpoint}/{tripleStore}/{userId}/{datasetName}/{sparqlName}")
  @Produces(MediaType.APPLICATION_JSON)
  @JacksonFeatures(
      serializationEnable = {SerializationFeature.INDENT_OUTPUT},
      serializationDisable = {SerializationFeature.WRITE_DATES_AS_TIMESTAMPS})
  public Response getDataset(@PathParam("endpoint") String endpointId,
                             @PathParam("tripleStore") String tripleStoreId,
                             @PathParam("userId") String userId,
                             @PathParam("datasetName") String datasetName,
                             @PathParam("sparqlName") String sparqlName) {
    return withRequest(endpointId, tripleStoreId, userId, datasetName, sparqlName, this::getSync);
  }

  @POST
  @Path("{endpoint}/{tripleStore}/{userId}/{datasetName}/{sparqlName}")
  @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
  public Response setDatasetProperties(@PathParam("endpoint") String endpointId,
                                       @PathParam("tripleStore") String tripleStoreId,
                                       @PathParam("userId") String userId,
                                       @PathParam("datasetName") String datasetName,
                                       @PathParam("sparqlName") String sparqlName,
                                       @FormParam("graphUri") String graphUri,
                                       @FormParam("autoSync") boolean autoSync) {
    return withRequest(endpointId, tripleStoreId, userId, datasetName, sparqlName, sync -> {
      sync.updateGraphUri(graphUri);
      sync.updateAutoSync(autoSync);
      return updateSync(sync, uriBuilder ->
          buildUri(uriBuilder, endpointId, tripleStoreId, userId, datasetName, sparqlName));
    });
  }

  @POST
  @Path("{endpoint}/{tripleStore}/{sparqlName}/sync")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response autoSyncDataset(@PathParam("endpoint") String endpointId,
                                  @PathParam("tripleStore") String tripleStoreId,
                                  @PathParam("sparqlName") String sparqlName,
                                  TimbuctooSync timbuctooSync) {
    return withRequest(endpointId, tripleStoreId,
        timbuctooSync.getUserId(), timbuctooSync.getDatasetName(), sparqlName, sync -> ifSyncExists(sync, () -> {
          if (sync.isAutoSync()) {
            return startSync(sync,
                uriBuilder -> buildUri(uriBuilder, endpointId, tripleStoreId, timbuctooSync.getUserId(),
                    timbuctooSync.getDatasetName(), sparqlName));
          }
          return Response.status(Response.Status.BAD_REQUEST).build();
        }));
  }

  @POST
  @Path("{endpoint}/{tripleStore}/{userId}/{datasetName}/{sparqlName}/sync")
  public Response syncDataset(@PathParam("endpoint") String endpointId,
                              @PathParam("tripleStore") String tripleStoreId,
                              @PathParam("userId") String userId,
                              @PathParam("datasetName") String datasetName,
                              @PathParam("sparqlName") String sparqlName) {
    return withRequest(endpointId, tripleStoreId, userId, datasetName, sparqlName, sync ->
        startSync(sync, uriBuilder ->
            buildUri(uriBuilder, endpointId, tripleStoreId, userId, datasetName, sparqlName)));
  }

  private URI buildUri(UriBuilder uriBuilder, String endpointId, String tripleStoreId,
                       String userId, String datasetName, String sparqlName) {
    return uriBuilder.path(endpointId).path(tripleStoreId)
                     .path(userId).path(datasetName).path(sparqlName).build();
  }

  private Response withRequest(String endpointId, String tripleStoreId,
                               String userId, String datasetName, String sparqlName,
                               WithSyncRequest<TimbuctooSparqlSync> withSync) {
    return createResponseFor(() -> {
      Optional<TimbuctooEndpoint> timbuctooEndpoint = findEndpoint(endpointId);
      Optional<TripleStore> tripleStore = findTripleStore(tripleStoreId);

      if (timbuctooEndpoint.isPresent() && tripleStore.isPresent()) {
        return TimbuctooSparqlSync.create(timbuctooEndpoint.get(), tripleStore.get(), userId, datasetName, sparqlName);
      }
      return null;
    }, withSync);
  }

  private record TimbuctooSync(String dataSetId) {
    @JsonCreator
    private TimbuctooSync(@JsonProperty("dataSetId") String dataSetId) {
      this.dataSetId = dataSetId;
    }

    public String getUserId() {
      return dataSetId.split("__", 2)[0];
    }

    public String getDatasetName() {
      return dataSetId.split("__", 2)[1];
    }
  }
}
