package nl.knaw.huc.di.kabara.rdfprocessing.rdf4j;

import nl.knaw.huc.di.kabara.rdfprocessing.rdf4j.parsers.NquadsUdParser;
import org.eclipse.rdf4j.rio.RDFParserRegistry;

public class Rdf4jIoFactory {
  private static Rdf4jRdfParser rdfParser = new Rdf4jRdfParser();

  private String rdfFormat = "application/n-quads"; // format for serializer

  public Rdf4jIoFactory() {
    RDFParserRegistry.getInstance().add(new NquadsUdParser.NquadsUdParserFactory());
  }

  public Rdf4jRdfParser makeRdfParser() {
    return rdfParser;
  }

}
