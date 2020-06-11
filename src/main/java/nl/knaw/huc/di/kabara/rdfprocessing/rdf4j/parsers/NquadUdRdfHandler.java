package nl.knaw.huc.di.kabara.rdfprocessing.rdf4j.parsers;

import nl.knaw.huc.di.kabara.rdfprocessing.RdfProcessingFailedException;
import nl.knaw.huc.di.kabara.rdfprocessing.RdfProcessor;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.util.function.Supplier;

public class NquadUdRdfHandler extends AbstractRDFHandler {
  private static final int ADD = '+';
  private final RdfProcessor rdfProcessor;
  private final String defaultGraph;
  private Supplier<Integer> actionSupplier;

  public NquadUdRdfHandler(RdfProcessor rdfProcessor, String defaultGraph) {
    this.rdfProcessor = rdfProcessor;
    this.defaultGraph = defaultGraph;
  }

  public void registerActionSupplier(Supplier<Integer> actionSupplier) {
    this.actionSupplier = actionSupplier;
  }

  @Override
  public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
    try {
      rdfProcessor.setPrefix(prefix, uri);
    } catch (RdfProcessingFailedException e) {
      throw new RDFHandlerException(e);
    }
  }

  @Override
  public void startRDF() throws RDFHandlerException {
  }

  @Override
  public void endRDF() throws RDFHandlerException {
    try {
      rdfProcessor.commit();
    } catch (RdfProcessingFailedException e) {
      throw new RDFHandlerException(e);
    }
  }

  @Override
  public void handleStatement(Statement st) throws RDFHandlerException {
    try {
      if (Thread.currentThread().isInterrupted()) {
        rdfProcessor.commit();
        throw new RDFHandlerException("Interrupted");
      }

      String graph = st.getContext() == null ? defaultGraph : st.getContext().stringValue();
      rdfProcessor.onQuad(
          isAssertion(),
          handleNode(st.getSubject()),
          st.getPredicate().stringValue(),
          handleNode(st.getObject()),
          (st.getObject() instanceof Literal) ? ((Literal) st.getObject()).getDatatype().toString() : null,
          (st.getObject() instanceof Literal) ? ((Literal) st.getObject()).getLanguage().orElse(null) : null,
          graph
      );
    } catch (RdfProcessingFailedException e) {
      throw new RDFHandlerException(e);
    }
  }

  private boolean isAssertion() {
    return actionSupplier == null || actionSupplier.get() == ADD;
  }

  private String handleNode(Value resource) {
    return resource.stringValue();
  }
}
