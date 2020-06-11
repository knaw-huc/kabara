package nl.knaw.huc.di.kabara.rdfprocessing;

public interface RdfProcessor {
  String LANGSTRING = "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString";

  void setPrefix(String prefix, String iri) throws RdfProcessingFailedException;

  void addRelation(String subject, String predicate, String object, String graph)
      throws RdfProcessingFailedException;

  void addValue(String subject, String predicate, String value, String dataType, String graph)
      throws RdfProcessingFailedException;

  void addLanguageTaggedString(String subject, String predicate, String value, String language,
                               String graph) throws RdfProcessingFailedException;

  void delRelation(String subject, String predicate, String object, String graph)
      throws RdfProcessingFailedException;

  void delValue(String subject, String predicate, String value, String valueType, String graph)
      throws RdfProcessingFailedException;

  void delLanguageTaggedString(String subject, String predicate, String value, String language,
                               String graph) throws RdfProcessingFailedException;

  default void onQuad(boolean isAssertion, String subject, String predicate, String object,
                      String dataType, String language, String graph) throws RdfProcessingFailedException {
    if (isAssertion) {
      if (dataType == null || dataType.isEmpty()) {
        this.addRelation(subject, predicate, object, graph);
      } else {
        if (hasLanguageAnnotation(dataType, language)) {
          this.addLanguageTaggedString(subject, predicate, object, language, graph);
        } else {
          this.addValue(subject, predicate, object, dataType, graph);
        }
      }
    } else {
      if (dataType == null || dataType.isEmpty()) {
        this.delRelation(subject, predicate, object, graph);
      } else {
        if (hasLanguageAnnotation(dataType, language)) {
          this.delLanguageTaggedString(subject, predicate, object, language, graph);
        } else {
          this.delValue(subject, predicate, object, dataType, graph);
        }
      }
    }
  }

  default boolean hasLanguageAnnotation(String dataType, String language) {
    return language != null && !language.isEmpty() && dataType.equals(LANGSTRING);
  }

  void commit() throws RdfProcessingFailedException;
}
