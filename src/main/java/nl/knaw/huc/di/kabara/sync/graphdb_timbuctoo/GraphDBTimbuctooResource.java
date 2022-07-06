package nl.knaw.huc.di.kabara.sync.graphdb_timbuctoo;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;
import nl.knaw.huc.di.kabara.KabaraConfiguration;
import nl.knaw.huc.di.kabara.endpoints.GraphDB;
import nl.knaw.huc.di.kabara.endpoints.TimbuctooEndpoint;
import nl.knaw.huc.di.kabara.sync.SyncResource;

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

@Path("/graphdb/timbuctoo/")
@Produces(MediaType.APPLICATION_JSON)
public class GraphDBTimbuctooResource extends SyncResource {
  public GraphDBTimbuctooResource(ExecutorService executor, KabaraConfiguration configuration) {
    super(executor, configuration, "/graphdb/timbuctoo/");
  }

  @GET
  @Path("{graphDB}/{endpoint}/{userId}/{datasetName}/{sparqlName}")
  @Produces(MediaType.APPLICATION_JSON)
  @JacksonFeatures(
      serializationEnable = {SerializationFeature.INDENT_OUTPUT},
      serializationDisable = {SerializationFeature.WRITE_DATES_AS_TIMESTAMPS})
  public Response getDataset(@PathParam("graphDB") String graphDbId,
                             @PathParam("endpoint") String endpointId,
                             @PathParam("userId") String userId,
                             @PathParam("datasetName") String datasetName,
                             @PathParam("sparqlName") String sparqlName) {
    return withRequest(graphDbId, endpointId, userId, datasetName, sparqlName, this::getSync);
  }

  @POST
  @Path("{graphDB}/{endpoint}/{userId}/{datasetName}/{sparqlName}")
  @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
  public Response setDatasetProperties(@PathParam("graphDB") String graphDbId,
                                       @PathParam("endpoint") String endpointId,
                                       @PathParam("userId") String userId,
                                       @PathParam("datasetName") String datasetName,
                                       @PathParam("sparqlName") String sparqlName,
                                       @FormParam("authorization") String authorization) {
    return withRequest(graphDbId, endpointId, userId, datasetName, sparqlName, sync -> {
      sync.updateAuthorization(authorization);
      return updateSync(sync, uriBuilder ->
          buildUri(uriBuilder, graphDbId, endpointId, userId, datasetName, sparqlName));
    });
  }

  @POST
  @Path("{graphDB}/{endpoint}/{userId}/{datasetName}/{sparqlName}/sync")
  public Response syncDataset(@PathParam("graphDB") String graphDbId,
                              @PathParam("endpoint") String endpointId,
                              @PathParam("userId") String userId,
                              @PathParam("datasetName") String datasetName,
                              @PathParam("sparqlName") String sparqlName) {
    return withRequest(graphDbId, endpointId, userId, datasetName, sparqlName, sync ->
        startSync(sync, uriBuilder -> buildUri(uriBuilder, graphDbId, endpointId, userId, datasetName, sparqlName)));
  }

  private URI buildUri(UriBuilder uriBuilder, String graphDbId, String endpointId,
                       String userId, String datasetName, String sparqlName) {
    return uriBuilder.path(graphDbId).path(endpointId)
                     .path(userId).path(datasetName).path(sparqlName).build();
  }

  private Response withRequest(String graphDbId, String endpointId, String userId,
                               String datasetName, String sparqlName, WithSyncRequest<GraphDBTimbuctooSync> withSync) {
    return createResponseFor(() -> {
      Optional<GraphDB> graphDB = findGraphDb(graphDbId);
      Optional<TimbuctooEndpoint> timbuctooEndpoint = findEndpoint(endpointId);

      if (graphDB.isPresent() && timbuctooEndpoint.isPresent()) {
        return GraphDBTimbuctooSync.create(graphDB.get(), timbuctooEndpoint.get(), userId, datasetName, sparqlName);
      }
      return null;
    }, withSync);
  }
}
