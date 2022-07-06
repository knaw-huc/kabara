package nl.knaw.huc.di.kabara.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SyncManager {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final Path path;

  static {
    OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    OBJECT_MAPPER.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
  }

  @JsonCreator
  public SyncManager(@JsonProperty("path") String path) {
    this.path = Paths.get(path);
    synchronized (this.path) {
      if (!Files.exists(this.path)) {
        try {
          Files.createDirectories(this.path);
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
      }
    }
  }

  public <S extends Sync> boolean syncExists(S sync) {
    Path file = sync.getFile(path);
    return Files.exists(file);
  }

  public void updateSync(Sync sync) throws IOException {
    synchronized (path) {
      Path file = sync.getFile(path);
      OBJECT_MAPPER.writeValue(file.toFile(), sync);
    }
  }

  public <S extends Sync> S reloadSync(S sync) throws IOException {
    synchronized (path) {
      Path file = sync.getFile(path);
      return (S) OBJECT_MAPPER.readValue(file.toFile(), sync.getClass());
    }
  }
}
