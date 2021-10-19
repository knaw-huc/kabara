package nl.knaw.huc.di.kabara.run;

import nl.knaw.huc.di.kabara.status.DataSetStatus;
import nl.knaw.huc.di.kabara.status.DataSetStatusManager;
import nl.knaw.huc.di.kabara.triplestore.TripleStore;

import java.io.IOException;
import java.util.Date;

public class DatasetRunnableFactory {
  private final TripleStore tripleStore;
  private final int timeout;
  private final DataSetStatusManager dataSetStatusManager;

  public DatasetRunnableFactory(TripleStore tripleStore, int timeout, DataSetStatusManager dataSetStatusManager) {
    this.tripleStore = tripleStore;
    this.timeout = timeout;
    this.dataSetStatusManager = dataSetStatusManager;
  }

  public DatasetRunnable createRunnable(String dataset) throws IOException {
    DataSetStatus dataSetStatus = dataSetStatusManager.getStatusOrCreate(dataset);
    return new DatasetRunnable(tripleStore, timeout, dataSetStatusManager, dataset, new Date(), dataSetStatus);
  }
}
