package nl.knaw.huc.di.kabara.rdfprocessing.rdf4j;

import nl.knaw.huc.di.kabara.rdfprocessing.RdfProcessingFailedException;
import nl.knaw.huc.di.kabara.rdfprocessing.RdfProcessor;
import nl.knaw.huc.di.kabara.rdfprocessing.rdf4j.parsers.NquadUdRdfHandler;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;

public class Rdf4jRdfParser {

  public void importRdf(
      InputStream input,
      String baseUri,
      String defaultGraph,
      RdfProcessor rdfProcessor,
      MediaType mimeType
  ) throws RdfProcessingFailedException {

    try {
      RDFFormat format = Rio.getParserFormatForMIMEType(mimeType.toString())
                            .orElseThrow(
          () -> new UnsupportedRDFormatException(mimeType + " is not a supported rdf type.")
        );
      RDFParser rdfParser = Rio.createParser(format);
      rdfParser.setPreserveBNodeIDs(true);
      rdfParser.setRDFHandler(new NquadUdRdfHandler(rdfProcessor, defaultGraph));
      rdfParser.parse(input, baseUri);
    } catch (IOException | UnsupportedRDFormatException e) {
      throw new RdfProcessingFailedException(e);
    } catch (RDFHandlerException e) {
      if (e.getCause() instanceof RdfProcessingFailedException) {
        throw (RdfProcessingFailedException) e.getCause();
      } else {
        throw new RdfProcessingFailedException(e);
      }
    }
  }
}
