package nl.knaw.huc.di.kabara.run;

import nl.knaw.huc.di.kabara.dataset.Dataset;
import nl.knaw.huc.di.kabara.dataset.DatasetManager;
import nl.knaw.huc.di.kabara.triplestore.TripleStore;

import java.io.IOException;
import java.util.Date;

public class DatasetRunnableFactory {
  private final TripleStore tripleStore;
  private final int timeout;
  private final DatasetManager datasetManager;

  public DatasetRunnableFactory(TripleStore tripleStore, int timeout, DatasetManager datasetManager) {
    this.tripleStore = tripleStore;
    this.timeout = timeout;
    this.datasetManager = datasetManager;
  }

  public DatasetRunnable createRunnable(Dataset dataset) throws IOException {
    return new DatasetRunnable(tripleStore, datasetManager, timeout, new Date(), dataset);
  }
}
