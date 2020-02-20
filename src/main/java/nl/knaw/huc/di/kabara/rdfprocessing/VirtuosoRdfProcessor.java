package nl.knaw.huc.di.kabara.rdfprocessing;

import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import java.io.IOException;

public class VirtuosoRdfProcessor implements RdfProcessor {


  private final SparqlSender spraqlSender;

  public VirtuosoRdfProcessor(SparqlSender spraqlSender) {

    this.spraqlSender = spraqlSender;
  }


  @Override
  public void setPrefix(String prefix, String iri) throws RdfProcessingFailedException {
    // Nothing to do
  }

  @Override
  public void addRelation(String subject, String predicate, String object, String graph)
      throws RdfProcessingFailedException {

    String sparql = "INSERT DATA\n" +
        "{\n" +
        "    GRAPH " + handleUri(graph) + " {\n" +
        "            " + handleUri(subject) + "\n" +
        "            " + handleUri(predicate) + "\n" +
        "            " + handleUri(object) + " .\n" +
        "    }\n" +
        "}";

    try {
      spraqlSender.sendSparql(sparql);
    } catch (IOException e) {
      throw new RdfProcessingFailedException(e);
    }
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
    String sparql = "INSERT DATA\n" +
        "{\n" +
        "    GRAPH " + handleUri(graph) + " {\n" +
        "            " + handleUri(subject) + "\n" +
        "            " + handleUri(predicate) + "\n" +
        "            \"" + escapceRdf(value) + "\"" + (valueType != null ? "^^" + handleUri(valueType) : "") + " .\n" +
        "    }\n" +
        "}";

    try {
      spraqlSender.sendSparql(sparql);
    } catch (IOException e) {
      throw new RdfProcessingFailedException(e);
    }
  }

  private String escapceRdf(String value) {
    return NTriplesUtil.escapeString(value);
  }

  @Override
  public void addLanguageTaggedString(String subject, String predicate, String value, String language, String graph)
      throws RdfProcessingFailedException {


    try {
      String sparql = "INSERT DATA\n" +
          "{\n" +
          "    GRAPH " + handleUri(graph) + " {\n" +
          "            " + handleUri(subject) + "\n" +
          "            " + handleUri(predicate) + "\n" +
          "            \"" + escapceRdf(value) + "\"@" + language + " .\n" +
          "    }\n" +
          "}";

      spraqlSender.sendSparql(sparql);
    } catch (IOException e) {
      throw new RdfProcessingFailedException(e);
    }
  }

  @Override
  public void delRelation(String subject, String predicate, String object, String graph)
      throws RdfProcessingFailedException {
    String sparql = "DELETE DATA\n" +
        "{\n" +
        "    GRAPH " + handleUri(graph) + " {\n" +
        "            " + handleUri(subject) + "\n" +
        "            " + handleUri(predicate) + "\n" +
        "            " + handleUri(object) + " .\n" +
        "    }\n" +
        "}";

    try {
      spraqlSender.sendSparql(sparql);
    } catch (IOException e) {
      throw new RdfProcessingFailedException(e);
    }
  }

  @Override
  public void delValue(String subject, String predicate, String value, String valueType, String graph)
      throws RdfProcessingFailedException {
    String sparql = "DELETE DATA\n" +
        "{\n" +
        "    GRAPH " + handleUri(graph) + " {\n" +
        "            " + handleUri(subject) + "\n" +
        "            " + handleUri(predicate) + "\n" +
        "            \"" + escapceRdf(value) + "\"" + (valueType != null ? "^^" + handleUri(valueType) : "") + " .\n" +
        "    }\n" +
        "}";

    try {
      spraqlSender.sendSparql(sparql);
    } catch (IOException e) {
      throw new RdfProcessingFailedException(e);
    }
  }

  @Override
  public void delLanguageTaggedString(String subject, String predicate, String value, String language, String graph)
      throws RdfProcessingFailedException {
    String sparql = "DELETE DATA\n" +
        "{\n" +
        "    GRAPH " + handleUri(graph) + " {\n" +
        "            " + handleUri(subject) + "\n" +
        "            " + handleUri(predicate) + "\n" +
        "            \"" + escapceRdf(value) + "\"@" + language + " .\n" +
        "    }\n" +
        "}";

    try {
      spraqlSender.sendSparql(sparql);
    } catch (IOException e) {
      throw new RdfProcessingFailedException(e);
    }
  }

  @Override
  public void commit() throws RdfProcessingFailedException {
    // nothing to do
  }

  public interface SparqlSender {
    void sendSparql(String sparqlMutation) throws IOException;
  }
}
