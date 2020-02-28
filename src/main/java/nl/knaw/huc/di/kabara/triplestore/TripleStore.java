package nl.knaw.huc.di.kabara.triplestore;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.lifecycle.Managed;

import java.io.IOException;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface TripleStore extends Managed {
  void sendSparQlUpdate(String sparQl) throws IOException;
}
