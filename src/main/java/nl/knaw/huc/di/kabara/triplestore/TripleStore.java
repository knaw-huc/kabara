package nl.knaw.huc.di.kabara.triplestore;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.lifecycle.Managed;
import nl.knaw.huc.di.kabara.rdfprocessing.RdfProcessor;
import nl.knaw.huc.di.kabara.status.DataSetStatusUpdater;

import java.io.IOException;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface TripleStore extends Managed {
  void sendSparqlUpdate(String sparql) throws IOException;

  RdfProcessor createRdfProcessor(DataSetStatusUpdater dataSetStatusUpdater);
}
