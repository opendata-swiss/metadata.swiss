package swiss.opendata.piveau.pipe.module.patching;

import io.piveau.rdf.Piveau;
import io.vertx.core.json.JsonArray;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.DCAT;
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

        assertFalse(model.contains(showcaseIri, RDF.type, model.createResource("http://localhost:3000/Showcase")), "Showcase type <http://localhost:3000/Showcase> should be removed");

        assertTrue(model.contains(showcaseIri, RDF.type, model.createResource("https://example.org/Showcase")), "Showcase type <https://example.org/Showcase> should be present");

        assertTrue(model.contains(showcaseIri, RDF.type, model.createResource("https://piveau.eu/ns/voc#CustomResource")), "CustomResource type <https://piveau.eu/ns/voc#CustomResource> should be preserved");
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
