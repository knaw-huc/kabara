package nl.knaw.huc.di.kabara.status;

import java.io.IOException;

@FunctionalInterface
public interface DataSetStatusUpdater {
  void updateStatus(String statusUpdate) throws IOException;
}
