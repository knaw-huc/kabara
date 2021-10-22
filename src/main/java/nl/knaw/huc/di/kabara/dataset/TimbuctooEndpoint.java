package nl.knaw.huc.di.kabara.dataset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TimbuctooEndpoint {
  private final String id;
  private final String url;

  @JsonCreator
  public TimbuctooEndpoint(@JsonProperty("id") String id, @JsonProperty("url") String url) {
    this.id = id;
    this.url = url;
  }

  public String getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }
}
