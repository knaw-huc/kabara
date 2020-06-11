package nl.knaw.huc.di.kabara.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class DataSetStatusManager {
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final Path path;

  @JsonCreator
  public DataSetStatusManager(@JsonProperty("path") String path) {
    this.path = Paths.get(path);
  }

  public Optional<DataSetStatus> getStatus(String dataSetUri) throws IOException {
    if (!StringUtils.isBlank(dataSetUri)) {
      final File dataSetStatusFile = path.resolve(URLEncoder.encode(dataSetUri, "UTF-8")).toFile();
      if (dataSetStatusFile.exists()) {
        synchronized (path) {
          final DataSetStatus dataSetStatus = OBJECT_MAPPER.readValue(dataSetStatusFile, DataSetStatus.class);

          return Optional.of(dataSetStatus);
        }
      } else {
        return Optional.empty();
      }
    }
    throw new IllegalArgumentException("dataSetUri does not contain is blank");

  }

  public DataSetStatus getStatusOrCreate(String dataSetUri) throws IOException {
    if (!StringUtils.isBlank(dataSetUri)) {
      final File dataSetStatusFile = path.resolve(URLEncoder.encode(dataSetUri, "UTF-8")).toFile();
      if (dataSetStatusFile.exists()) {
        synchronized (path) {
          return OBJECT_MAPPER.readValue(dataSetStatusFile, DataSetStatus.class);
        }
      } else {
        final DataSetStatus newDataSetStatus = DataSetStatus.createNewDataSetStatus();
        updateStatus(dataSetUri, newDataSetStatus);
        return newDataSetStatus;
      }
    }
    throw new IllegalArgumentException("dataSetUri does not contain is blank");
  }

  public void updateStatus(String dataSetUri, DataSetStatus dataSetStatus) throws IOException {
    if (!StringUtils.isBlank(dataSetUri)) {
      synchronized (path) {
        if (!path.toFile().exists()) {
          path.toFile().mkdirs();
        }
        final File dataSetStatusFile = path.resolve(URLEncoder.encode(dataSetUri, "UTF-8")).toFile();
        OBJECT_MAPPER.writeValue(dataSetStatusFile, dataSetStatus);
      }
    }
  }
}
