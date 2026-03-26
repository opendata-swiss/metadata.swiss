package swiss.opendata.piveau.pipe.module.patching;

import io.piveau.rdf.Piveau;
import io.vertx.core.json.JsonArray;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PayloadPatcherTest {

    @Test
    public void testAction_remove_dataset_cloak() throws Exception {
        Model model = loadModel("showcase-mietpreisentwicklung.ttl");

        JsonArray actions = new JsonArray().add("remove-dataset-cloak");

        PayloadPatcher.apply(model, actions, null);

        final Resource showcaseIri = model.createResource("http://localhost:3000/showcase/mietpreisentwicklung-in-bern");

        assertFalse(model.contains(showcaseIri, RDF.type, DCAT.Dataset), "dcat:Dataset type should be removed");
    }

    @Test
    public void testAction_fix_showcase_typing() throws Exception {
        Model model = loadModel("showcase-mietpreisentwicklung.ttl");

        JsonArray actions = new JsonArray().add("fix-showcase-typing");

        PayloadPatcher.apply(model, actions, null);

        final Resource showcaseIri = model.createResource("http://localhost:3000/showcase/mietpreisentwicklung-in-bern");

        assertFalse(model.contains(showcaseIri, RDF.type, model.createResource(PayloadPatcher.SHOWCASE_TMP_URI)), "Showcase type <http://localhost:3000/Showcase> should be removed");

        assertTrue(model.contains(showcaseIri, RDF.type, model.createResource(PayloadPatcher.SHOWCASE_TARGET_URI)), "Showcase type <https://example.org/Showcase> should be present");

        assertTrue(model.contains(showcaseIri, RDF.type, model.createResource("https://piveau.eu/ns/voc#CustomResource")), "CustomResource type <https://piveau.eu/ns/voc#CustomResource> should be preserved");
    }

    @Test
    public void testAction_unwrap_references() throws Exception {
        Model model = loadModel("showcase-mietpreisentwicklung.ttl");

        JsonArray actions = new JsonArray().add("unwrap-references");

        PayloadPatcher.apply(model, actions, null);

        final Resource showcaseIri = model.createResource("http://localhost:3000/showcase/mietpreisentwicklung-in-bern");

        assertTrue(model.contains(showcaseIri, DCTerms.references, model.createResource("https://opendata.swiss/set/data/523-staatskanzlei-kanton-zuerich")), "Referenced IRI <https://opendata.swiss/set/data/523-staatskanzlei-kanton-zuerich> should be unwrapped from bNode");

        for (StmtIterator it = model.listStatements(showcaseIri, DCTerms.references, (RDFNode) null); it.hasNext(); ) {
            final Statement stmt = it.next();
            assertFalse(stmt.getObject().isAnon(), "bNode should be removed");
        }
        assertFalse(model.contains(null, DCTerms.identifier, "https://opendata.swiss/set/data/523-staatskanzlei-kanton-zuerich"), "Wrapped IRI \"https://opendata.swiss/set/data/523-staatskanzlei-kanton-zuerich\" should be removed");
    }

    @Test
    public void testAction_unwrap_references_skips_non_uri_references() throws Exception {
        Model model = loadModel("showcase-without-uri-references.ttl");

        JsonArray actions = new JsonArray().add("unwrap-references");

        PayloadPatcher.apply(model, actions, null);

        final Resource showcaseIri = model.createResource("http://localhost:3000/showcase/mietpreisentwicklung-in-bern");

        for (StmtIterator it = model.listStatements(showcaseIri, DCTerms.references, (RDFNode) null); it.hasNext(); ) {
            final Statement stmt = it.next();
            Resource bNode = stmt.getObject().asResource();

            assertTrue(bNode.isAnon(), "bNode reference should be preserved");

            assertTrue(model.contains(bNode, DCTerms.identifier, "$$$://foo"), "Non-URI identifier should be preserved");
        }
    }

    private Model loadModel(String fileName) throws IOException {
        String inputRdfTurtle = loadFile(fileName);
        Model result = Piveau.toModel(inputRdfTurtle.getBytes(), Lang.TURTLE);
        return result;
    }

    private String loadFile(String fileName) throws java.io.IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
        if (is == null) throw new RuntimeException(String.format("Resource not found: %s", fileName));

        return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
