package swiss.opendata.piveau.testbench.utils;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.Set;

public class SideEffectUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SideEffectUtils.class);

    /**
     * Checks if a SPARQL ASK query returns true.
     * @param sparqlEndpoint The URL of the SPARQL endpoint (e.g. http://localhost:7200/repositories/piveau)
     * @param askQuery The SPARQL ASK query
     * @return true if the query returns true
     */
    public static boolean checkSparqlAsk(String sparqlEndpoint, String askQuery) {
        LOG.info("Executing SPARQL ASK on {}: {}", sparqlEndpoint, askQuery);
        SPARQLRepository repo = new SPARQLRepository(sparqlEndpoint);
        try {
            repo.init();
            try (var conn = repo.getConnection()) {
                BooleanQuery query = conn.prepareBooleanQuery(askQuery);
                boolean result = query.evaluate();
                LOG.info("SPARQL ASK Result: {}", result);
                return result;
            }
        } catch (Exception e) {
            LOG.error("Failed to execute SPARQL ASK: {}", e.getMessage());
            throw new RuntimeException("SPARQL check failed", e);
        } finally {
            repo.shutDown();
        }
    }

    /**
     * Extracts the subject IRI of the first resource with the given rdf:type from a Turtle string.
     *
     * @param turtle  The RDF content as Turtle
     * @param typeIri The full IRI of the rdf:type to look for (e.g. "http://www.w3.org/ns/dcat#Dataset")
     * @return The subject IRI string
     * @throws RuntimeException if no matching subject is found or parsing fails
     */
    public static String extractSubjectIri(String turtle, String typeIri) {
        try {
            Model model = Rio.parse(new StringReader(turtle), "", RDFFormat.TURTLE);
            IRI typeValue = SimpleValueFactory.getInstance().createIRI(typeIri);
            Set<Resource> subjects = model.filter(null, RDF.TYPE, typeValue).subjects();
            if (subjects.isEmpty()) {
                throw new RuntimeException("No subject found with rdf:type <" + typeIri + "> in response");
            }
            if (subjects.size() > 1) {
                throw new RuntimeException("Multiple subjects found with rdf:type <" + typeIri + ">: " + subjects);
            }
            Resource subject = subjects.iterator().next();
            if (!(subject instanceof IRI)) {
                throw new RuntimeException("Subject is a blank node, not an IRI");
            }
            String iri = subject.toString();
            LOG.info("Extracted IRI for type <{}>: {}", typeIri, iri);
            return iri;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Turtle and extract IRI: " + e.getMessage(), e);
        }
    }

    public static void logRepositoryContent(String sparqlEndpoint) {
        SPARQLRepository repo = new SPARQLRepository(sparqlEndpoint);
        try {
            repo.init();
            try (var conn = repo.getConnection()) {
                var tupleQuery = conn.prepareTupleQuery("SELECT * WHERE { GRAPH ?g { ?s ?p ?o } } LIMIT 20");
                try (var result = tupleQuery.evaluate()) {
                    LOG.info("--- GRAPHDB DUMP (Limit 20) ---");
                    while (result.hasNext()) {
                        LOG.info("Triple: {}", result.next());
                    }
                    LOG.info("-------------------------------");
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to dump repo: {}", e.getMessage());
        } finally {
            repo.shutDown();
        }
    }
}
