package nl.knaw.huc.di.kabara.endpoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;

public class GraphDB extends TripleStore {
  private static final ValueFactory VALUE_FACTORY = SimpleValueFactory.getInstance();
  private static final String CHANGES_QUERY = """
      PREFIX hist: <http://www.ontotext.com/at/>
      SELECT * {
          ?log a hist:history ;
               hist:parameters (%s %s) ;
               hist:insert ?isAssertion ;
               hist:subject ?subject ;
               hist:predicate ?predicate ;
               hist:object ?object ;
               hist:graph ?graph
      }
      """;
  private static final String TRIM_HISTORY_QUERY = """
      PREFIX hist: <http://www.ontotext.com/at/>
      INSERT DATA {
          [] hist:trimBefore %s  .
      }
      """;

  @JsonCreator
  public GraphDB(
      @JsonProperty("id") String id,
      @JsonProperty("url") String url,
      @JsonProperty("user") String user,
      @JsonProperty("password") String password,
      @JsonProperty("sparqlPath") String sparqlPath,
      @JsonProperty("sparqlWritePath") String sparqlWritePath,
      @JsonProperty("batchSize") int batchSize) {
    super(id, url, user, password, sparqlPath, sparqlWritePath, batchSize);
  }

  @Override
  public void sendSparqlUpdate(String sparqlName, String sparql) throws IOException {
    super.sendUrlEncodedSparqlUpdate(sparqlName, sparql);
  }

  public List<AssertedStatement> withChanges(String sparqlName, Date from, Date to) {
    Literal fromLiteral = VALUE_FACTORY.createLiteral(from != null ? from : new Date(0));
    Literal toLiteral = VALUE_FACTORY.createLiteral(to);

    URI endpoint = getEndpoint(sparqlName, false);
    Repository repository = new SPARQLRepository(endpoint.toString());
    try (final RepositoryConnection connection = repository.getConnection()) {
      TupleQuery tupleQuery = connection.prepareTupleQuery(String.format(CHANGES_QUERY, fromLiteral, toLiteral));
      tupleQuery.setIncludeInferred(false);

      try (final TupleQueryResult result = tupleQuery.evaluate()) {
        return result.stream().map(bs -> new AssertedStatement(
            ((Literal) bs.getValue("isAssertion")).booleanValue(),
            VALUE_FACTORY.createStatement(
                (Resource) bs.getValue("subject"),
                (IRI) bs.getValue("predicate"),
                bs.getValue("object"),
                bs.getValue("graph") != null ? (Resource) bs.getValue("graph") : null)
        )).toList();
      }
    }
  }

  public void deleteChanges(String sparqlName, Date upUntil) {
    Literal dateLiteral = VALUE_FACTORY.createLiteral(upUntil);

    URI endpoint = getEndpoint(sparqlName, true);
    Repository repository = new SPARQLRepository(endpoint.toString());
    try (final RepositoryConnection connection = repository.getConnection()) {
      Update update = connection.prepareUpdate(String.format(TRIM_HISTORY_QUERY, dateLiteral));
      update.execute();
    }
  }

  public record AssertedStatement(boolean isAssertion, Statement statement) {
  }
}
