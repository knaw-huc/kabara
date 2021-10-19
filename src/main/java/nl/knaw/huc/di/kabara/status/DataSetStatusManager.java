package nl.knaw.huc.di.kabara.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class DataSetStatusManager {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Path path;

  static {
    OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    OBJECT_MAPPER.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
  }

  @JsonCreator
  public DataSetStatusManager(@JsonProperty("path") String path) {
    this.path = Paths.get(path);
  }

  public Optional<DataSetStatus> getStatus(String dataSetUri) throws IOException {
    if (!StringUtils.isBlank(dataSetUri)) {
      final File dataSetStatusFile = getStatusFile(dataSetUri);
      if (dataSetStatusFile.exists()) {
        synchronized (path) {
          final DataSetStatus dataSetStatus = OBJECT_MAPPER.readValue(dataSetStatusFile, DataSetStatus.class);
          return Optional.of(dataSetStatus);
        }
      }

      return Optional.empty();
    }

    throw new IllegalArgumentException("dataSetUri is blank");
  }

  public DataSetStatus getStatusOrCreate(String dataSetUri) throws IOException {
    if (!StringUtils.isBlank(dataSetUri)) {
      final File dataSetStatusFile = getStatusFile(dataSetUri);
      if (dataSetStatusFile.exists()) {
        synchronized (path) {
          return OBJECT_MAPPER.readValue(dataSetStatusFile, DataSetStatus.class);
        }
      }

      final DataSetStatus newDataSetStatus = DataSetStatus.createNewDataSetStatus();
      updateStatus(dataSetUri, newDataSetStatus);
      return newDataSetStatus;
    }

    throw new IllegalArgumentException("dataSetUri is blank");
  }

  public void updateStatus(String dataSetUri, DataSetStatus dataSetStatus) throws IOException {
    if (!StringUtils.isBlank(dataSetUri)) {
      synchronized (path) {
        if (!path.toFile().exists()) {
          path.toFile().mkdirs();
        }

        final File dataSetStatusFile = getStatusFile(dataSetUri);
        OBJECT_MAPPER.writeValue(dataSetStatusFile, dataSetStatus);
      }
    }
  }

  private File getStatusFile(String dataSetUri) {
    return path.resolve(URLEncoder.encode(dataSetUri, StandardCharsets.UTF_8) + ".json").toFile();
  }
}
