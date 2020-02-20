package nl.knaw.huc.di.kabara;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

public class KabaraConfiguration extends Configuration {

  @NotEmpty
  private String template;
  @NotEmpty
  private String configFileName;

  @JsonProperty
  public String getTemplate() {
    return template;
  }

  @JsonProperty
  public void setTemplate(String template) {
    this.template = template;
  }

  @JsonProperty
  public void setConfigFileName(String configFileName) {
    this.configFileName = configFileName;
  }

  @JsonProperty
  public String getConfigFileName() {
    return configFileName;
  }
}
