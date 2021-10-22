package nl.knaw.huc.di.kabara.dataset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class DatasetManager {
  private final ObjectMapper objectMapper;
  private final Path path;

  @JsonCreator
  public DatasetManager(@JsonProperty("path") String path) {
    objectMapper = new ObjectMapper();
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));

    this.path = Paths.get(path);
    synchronized (this.path) {
      if (!this.path.toFile().exists()) {
        this.path.toFile().mkdirs();
      }
    }
  }

  public Optional<Dataset> getDataset(TimbuctooEndpoint endpoint,
                                      String userId, String datasetName) throws IOException {
    if (endpoint != null && !StringUtils.isBlank(userId) && !StringUtils.isBlank(datasetName)) {
      final File datasetFile = getDatasetFile(endpoint, userId, datasetName);
      if (datasetFile.exists()) {
        synchronized (path) {
          final Dataset dataset = objectMapper.readValue(datasetFile, Dataset.class);
          return Optional.of(dataset);
        }
      }
      return Optional.empty();
    }

    throw new IllegalArgumentException("Data contains blanks");
  }

  public Dataset getDatasetOrCreate(TimbuctooEndpoint endpoint, String userId, String datasetName) throws IOException {
    Optional<Dataset> dataset = getDataset(endpoint, userId, datasetName);
    if (dataset.isPresent()) {
      return dataset.get();
    }

    final Dataset newDataset = Dataset.createNewDataSetStatus(endpoint, userId, datasetName);
    updateDataset(newDataset);
    return newDataset;
  }

  public void updateDataset(Dataset dataset) throws IOException {
    synchronized (path) {
      final File datasetFile =
          getDatasetFile(dataset.getTimbuctooEndpoint(), dataset.getUserId(), dataset.getDatasetName());
      objectMapper.writeValue(datasetFile, dataset);
    }
  }

  public Dataset reloadDataset(Dataset dataset) throws IOException {
    synchronized (path) {
      final File datasetFile =
          getDatasetFile(dataset.getTimbuctooEndpoint(), dataset.getUserId(), dataset.getDatasetName());
      return objectMapper.readValue(datasetFile, Dataset.class);
    }
  }

  private File getDatasetFile(TimbuctooEndpoint endpoint, String userId, String datasetName) {
    String fileName = String.format("%s__%s__%s.json", endpoint.getId(), userId, datasetName);
    return path.resolve(fileName).toFile();
  }
}
