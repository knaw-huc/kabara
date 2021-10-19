package nl.knaw.huc.di.kabara.status;

import java.io.IOException;
import java.util.Date;

@FunctionalInterface
public interface DataSetLastUpdateWriter {
  void updateLastUpdate(Date dateTime) throws IOException;
}
