package nl.knaw.huc.di.kabara.processors;

import nl.knaw.huc.di.kabara.sync.SyncStatusUpdater;
import nl.knaw.huc.di.kabara.endpoints.TripleStore;
import nl.knaw.huc.rdf4j.rio.nquadsnd.RdfProcessingFailedException;
import nl.knaw.huc.rdf4j.rio.nquadsnd.RdfProcessor;
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
  private static final String SPARQL_UPDATE = """
       %s\s
       %s\s
       %s .\s
      """;

  private static final String SPARQL_GRAPH_UPDATE = """
      GRAPH %s {\s
       %s
      }\s
      """;

  private final TripleStore tripleStore;
  private final SyncStatusUpdater syncStatusUpdater;
  private final String sparqlName;
  private final int batchSize;

  private final List<String> deletions;
  private final List<String> inserts;

  private int counter;

  public SparqlUpdateRdfProcessor(TripleStore tripleStore, SyncStatusUpdater syncStatusUpdater,
                                  String sparqlName, int batchSize) {
    this.tripleStore = tripleStore;
    this.syncStatusUpdater = syncStatusUpdater;
    this.sparqlName = sparqlName;
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
    handleQuad(createSparqlQuery(handleUri(graph), handleUri(subject), handleUri(predicate), handleUri(object)), true);
  }

  public String handleUri(String uri) {
    if (uri == null) {
      return null;
    }
    if (uri.startsWith(BLANK_NODE)) {
      return uri; // is a blank node
    }
    return String.format(URI, uri);
  }

  @Override
  public void addValue(String subject, String predicate, String value, String valueType, String graph)
      throws RdfProcessingFailedException {
    String val = "\"" + escapeRdf(value) + "\"" + (valueType != null ? "^^" + handleUri(valueType) : "");
    handleQuad(createSparqlQuery(handleUri(graph), handleUri(subject), handleUri(predicate), val), true);
  }

  private String escapeRdf(String value) {
    return escapeString(value);
  }

  @Override
  public void addLanguageTaggedString(String subject, String predicate, String value, String language, String graph)
      throws RdfProcessingFailedException {
    String val = "\"" + escapeRdf(value) + "\"@" + language;
    handleQuad(createSparqlQuery(handleUri(graph), handleUri(subject), handleUri(predicate), val), true);
  }

  @Override
  public void delRelation(String subject, String predicate, String object, String graph)
      throws RdfProcessingFailedException {
    handleQuad(createSparqlQuery(handleUri(graph), handleUri(subject), handleUri(predicate), handleUri(object)), false);
  }

  @Override
  public void delValue(String subject, String predicate, String value, String valueType, String graph)
      throws RdfProcessingFailedException {
    String val = "\"" + escapeRdf(value) + "\"" + (valueType != null ? "^^" + handleUri(valueType) : "");
    handleQuad(createSparqlQuery(handleUri(graph), handleUri(subject), handleUri(predicate), val), false);
  }

  @Override
  public void delLanguageTaggedString(String subject, String predicate, String value, String language, String graph)
      throws RdfProcessingFailedException {
    String val = "\"" + escapeRdf(value) + "\"@" + language;
    handleQuad(createSparqlQuery(handleUri(graph), handleUri(subject), handleUri(predicate), val), false);
  }

  public void handleQuad(String quad, boolean isAssertion) throws RdfProcessingFailedException {
    try {
      counter++;
      if (isAssertion) {
        inserts.add(quad);
      } else {
        deletions.add(quad);
      }

      if (counter % batchSize == 0) {
        sendSparql();
      }

      if (counter % 10000 == 0) {
        updateStatus("Quads processed: " + counter);
      }

    } catch (IOException e) {
      updateException(e);
      throw new RdfProcessingFailedException(e);
    }
  }

  private String createSparqlQuery(String graph, String subject, String predicate, String object) {
    String sparqlUpdateQuery = String.format(SPARQL_UPDATE, subject, predicate, object);
    if (graph != null) {
      return String.format(SPARQL_GRAPH_UPDATE, graph, sparqlUpdateQuery);
    }
    return sparqlUpdateQuery;
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
      syncStatusUpdater.updateStatus(statusUpdate);
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
    tripleStore.sendSparqlUpdate(sparqlName, sparql.toString());
  }

  @Override
  public void commit() throws RdfProcessingFailedException {
    updateStatus("Quads processed: " + counter);
    try {
      sendSparql();
    } catch (IOException e) {
      updateException(e);
      throw new RdfProcessingFailedException(e);
    }
  }
}
