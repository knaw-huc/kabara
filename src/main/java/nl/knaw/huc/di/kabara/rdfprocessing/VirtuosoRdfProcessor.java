package nl.knaw.huc.di.kabara.rdfprocessing;

import nl.knaw.huc.di.kabara.status.DataSetStatusUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.rdf4j.rio.ntriples.NTriplesUtil.escapeString;

public class VirtuosoRdfProcessor implements RdfProcessor {
  public static final Logger LOG = LoggerFactory.getLogger(VirtuosoRdfProcessor.class);

  private final SparqlSender spraqlSender;
  private final DataSetStatusUpdater dataSetStatusUpdater;
  private final List<String> deletions;
  private final List<String> inserts;
  private int counter;

  public VirtuosoRdfProcessor(SparqlSender spraqlSender, DataSetStatusUpdater dataSetStatusUpdater) {
    this.spraqlSender = spraqlSender;
    this.dataSetStatusUpdater = dataSetStatusUpdater;
    inserts = new ArrayList<>();
    deletions = new ArrayList<>();
    counter = 0;
  }


  @Override
  public void setPrefix(String prefix, String iri) throws RdfProcessingFailedException {
    // Nothing to do
  }

  @Override
  public void addRelation(String subject, String predicate, String object, String graph)
      throws RdfProcessingFailedException {

    String sparql = "    GRAPH " + handleUri(graph) + " {\n" +
        "            " + handleUri(subject) + "\n" +
        "            " + handleUri(predicate) + "\n" +
        "            " + handleUri(object) + " .\n" +
        "    }\n";


    handleTriple(sparql, true);
  }

  public String handleUri(String uri) {
    if (uri.startsWith("_:")) {
      return uri; // is a blank node
    }
    return String.format("<%s>", uri);
  }

  @Override
  public void addValue(String subject, String predicate, String value, String valueType, String graph)
      throws RdfProcessingFailedException {
    String sparql = "    GRAPH " + handleUri(graph) + " {\n" +
        "            " + handleUri(subject) + "\n" +
        "            " + handleUri(predicate) + "\n" +
        "            \"" + escapeRdf(value) + "\"" + (valueType != null ? "^^" + handleUri(valueType) : "") + " .\n" +
        "    }\n";


    handleTriple(sparql, true);

  }

  private String escapeRdf(String value) {
    return escapeString(value);
  }

  @Override
  public void addLanguageTaggedString(String subject, String predicate, String value, String language, String graph)
      throws RdfProcessingFailedException {


    String sparql = "    GRAPH " + handleUri(graph) + " {\n" +
        "            " + handleUri(subject) + "\n" +
        "            " + handleUri(predicate) + "\n" +
        "            \"" + escapeRdf(value) + "\"@" + language + " .\n" +
        "    }\n";

    handleTriple(sparql, true);

  }

  @Override
  public void delRelation(String subject, String predicate, String object, String graph)
      throws RdfProcessingFailedException {
    String sparql = "    GRAPH " + handleUri(graph) + " {\n" +
        "            " + handleUri(subject) + "\n" +
        "            " + handleUri(predicate) + "\n" +
        "            " + handleUri(object) + " .\n" +
        "    }\n";


    handleTriple(sparql, false);
  }

  @Override
  public void delValue(String subject, String predicate, String value, String valueType, String graph)
      throws RdfProcessingFailedException {
    String sparql = "    GRAPH " + handleUri(graph) + " {\n" +
        "            " + handleUri(subject) + "\n" +
        "            " + handleUri(predicate) + "\n" +
        "            \"" + escapeRdf(value) + "\"" + (valueType != null ? "^^" + handleUri(valueType) : "") + " .\n" +
        "    }\n";


    handleTriple(sparql, false);
  }

  @Override
  public void delLanguageTaggedString(String subject, String predicate, String value, String language, String graph)
      throws RdfProcessingFailedException {
    String sparql = "    GRAPH " + handleUri(graph) + " {\n" +
        "            " + handleUri(subject) + "\n" +
        "            " + handleUri(predicate) + "\n" +
        "            \"" + escapeRdf(value) + "\"@" + language + " .\n" +
        "    }\n";


    handleTriple(sparql, false);
  }

  public void handleTriple(String triple, boolean isAssertion) throws RdfProcessingFailedException {
    try {
      counter++;
      if (isAssertion) {
        inserts.add(triple);
      } else {
        deletions.add(triple);
      }

      if (counter % 500 == 0) {
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
    updateStatus(exception.getMessage());
    LOG.error("Exception during process", exception);

    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);
    exception.printStackTrace(printWriter);
    updateStatus(stringWriter.toString());
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
      sparql.append("INSERT DATA\n")
            .append("{\n")
            .append(String.join("\n", inserts))
            .append("}");
    }
    if (!deletions.isEmpty()) {
      sparql.append("DELETE DATA\n")
            .append("{\n")
            .append(String.join("\n", deletions))
            .append("}");
    }
    inserts.clear();
    deletions.clear();
    spraqlSender.sendSparql(sparql.toString());
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

  public interface SparqlSender {
    void sendSparql(String sparql) throws IOException;
  }
}
