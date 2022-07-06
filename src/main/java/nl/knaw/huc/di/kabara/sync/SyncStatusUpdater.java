package nl.knaw.huc.di.kabara.sync;

import java.io.IOException;

@FunctionalInterface
public interface SyncStatusUpdater {
  void updateStatus(String statusUpdate) throws IOException;
}
