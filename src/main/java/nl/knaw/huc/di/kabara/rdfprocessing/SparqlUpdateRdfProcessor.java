package nl.knaw.huc.di.kabara.rdfprocessing;

import nl.knaw.huc.di.kabara.status.DataSetStatusUpdater;
import nl.knaw.huc.di.kabara.triplestore.TripleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.rdf4j.rio.helpers.NTriplesUtil.escapeString;

public class SparqlUpdateRdfProcessor implements RdfProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(SparqlUpdateRdfProcessor.class);

  private static final String BLANK_NODE = "_:";
  private static final String URI = "<%s>";
  private static final String SPARQL_UPDATE = "GRAPH %s { \n" +
      " %s \n" +
      " %s \n" +
      " %s . \n" +
      "} \n";

  private final TripleStore tripleStore;
  private final DataSetStatusUpdater dataSetStatusUpdater;
  private final int batchSize;

  private final List<String> deletions;
  private final List<String> inserts;

  private int counter;

  public SparqlUpdateRdfProcessor(TripleStore tripleStore, DataSetStatusUpdater dataSetStatusUpdater, int batchSize) {
    this.tripleStore = tripleStore;
    this.dataSetStatusUpdater = dataSetStatusUpdater;
    this.batchSize = batchSize;

    inserts = new ArrayList<>();
    deletions = new ArrayList<>();
    counter = 0;
  }

  @Override
  public void setPrefix(String prefix, String iri) {
    // Nothing to do
  }

  @Override
  public void addRelation(String subject, String predicate, String object, String graph)
      throws RdfProcessingFailedException {
    handleTriple(String.format(SPARQL_UPDATE,
        handleUri(graph), handleUri(subject), handleUri(predicate), handleUri(object)), true);
  }

  public String handleUri(String uri) {
    if (uri.startsWith(BLANK_NODE)) {
      return uri; // is a blank node
    }
    return String.format(URI, uri);
  }

  @Override
  public void addValue(String subject, String predicate, String value, String valueType, String graph)
      throws RdfProcessingFailedException {
    String val = "\"" + escapeRdf(value) + "\"" + (valueType != null ? "^^" + handleUri(valueType) : "");
    handleTriple(String.format(SPARQL_UPDATE,
        handleUri(graph), handleUri(subject), handleUri(predicate), val), true);
  }

  private String escapeRdf(String value) {
    return escapeString(value);
  }

  @Override
  public void addLanguageTaggedString(String subject, String predicate, String value, String language, String graph)
      throws RdfProcessingFailedException {
    String val = "\"" + escapeRdf(value) + "\"@" + language;
    handleTriple(String.format(SPARQL_UPDATE,
        handleUri(graph), handleUri(subject), handleUri(predicate), val), true);
  }

  @Override
  public void delRelation(String subject, String predicate, String object, String graph)
      throws RdfProcessingFailedException {
    handleTriple(String.format(SPARQL_UPDATE,
        handleUri(graph), handleUri(subject), handleUri(predicate), handleUri(object)), false);
  }

  @Override
  public void delValue(String subject, String predicate, String value, String valueType, String graph)
      throws RdfProcessingFailedException {
    String val = "\"" + escapeRdf(value) + "\"" + (valueType != null ? "^^" + handleUri(valueType) : "");
    handleTriple(String.format(SPARQL_UPDATE,
        handleUri(graph), handleUri(subject), handleUri(predicate), val), false);
  }

  @Override
  public void delLanguageTaggedString(String subject, String predicate, String value, String language, String graph)
      throws RdfProcessingFailedException {
    String val = "\"" + escapeRdf(value) + "\"@" + language;
    handleTriple(String.format(SPARQL_UPDATE,
        handleUri(graph), handleUri(subject), handleUri(predicate), val), false);
  }

  public void handleTriple(String triple, boolean isAssertion) throws RdfProcessingFailedException {
    try {
      counter++;
      if (isAssertion) {
        inserts.add(triple);
      } else {
        deletions.add(triple);
      }

      if (counter % batchSize == 0) {
        sendSparql();
      }

      if (counter % 10000 == 0) {
        updateStatus("Triples processed: " + counter);
      }

    } catch (IOException e) {
      updateException(e);
      throw new RdfProcessingFailedException(e);
    }
  }

  private void updateException(Exception exception) throws RdfProcessingFailedException {
    LOG.error("Exception during process", exception);
    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);
    exception.printStackTrace(printWriter);
    updateStatus(exception.getMessage() + " " + stringWriter);
  }

  private void updateStatus(String statusUpdate) throws RdfProcessingFailedException {
    try {
      LOG.info(statusUpdate);
      dataSetStatusUpdater.updateStatus(statusUpdate);
    } catch (IOException e) {
      throw new RdfProcessingFailedException(e);
    }
  }

  public void sendSparql() throws IOException {
    StringBuilder sparql = new StringBuilder();
    if (!inserts.isEmpty()) {
      sparql.append("INSERT DATA {\n")
            .append(String.join("\n", inserts))
            .append("} ; ");
    }
    if (!deletions.isEmpty()) {
      sparql.append("DELETE DATA {\n")
            .append(String.join("\n", deletions))
            .append("} ;");
    }
    inserts.clear();
    deletions.clear();
    tripleStore.sendSparqlUpdate(sparql.toString());
  }

  @Override
  public void commit() throws RdfProcessingFailedException {
    updateStatus("Triples processed: " + counter);
    try {
      sendSparql();
    } catch (IOException e) {
      updateException(e);
      throw new RdfProcessingFailedException(e);
    }
  }
}
