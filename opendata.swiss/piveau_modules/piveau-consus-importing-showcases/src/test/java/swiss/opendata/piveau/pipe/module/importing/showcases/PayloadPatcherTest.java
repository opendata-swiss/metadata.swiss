package swiss.opendata.piveau.pipe.module.importing.showcases;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PayloadPatcherTest {

    @Test
    public void testPayloadPatcherAppliesAllTransformations() throws Exception {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = getClass().getResourceAsStream("/showcase-mietpreisentwicklung.ttl")) {
            model.read(in, null, "TTL");
        }

        Resource showcase = model.createResource("http://localhost:3000/showcase/mietpreisentwicklung-in-bern");

        // Apply patches
        PayloadPatcher.apply(model, null);

        // 1. Assert remove-dataset-cloak
        assertFalse(model.contains(showcase, RDF.type, DCAT.Dataset), "dcat:Dataset type should be removed");

        // 2. Assert fix-showcase-typing
        assertFalse(model.contains(showcase, RDF.type, model.createResource(PayloadPatcher.SHOWCASE_TMP_URI)), "Localhost Showcase type should be removed");
        assertTrue(model.contains(showcase, RDF.type, model.createResource(PayloadPatcher.SHOWCASE_TARGET_URI)), "Target Showcase type should be present");

        // 3. Assert unwrap-references
        Resource targetRef = model.createResource("https://opendata.swiss/set/data/523-staatskanzlei-kanton-zuerich");
        assertTrue(model.contains(showcase, DCTerms.references, targetRef), "Referenced IRI should be unwrapped from bNode");

        for (StmtIterator it = model.listStatements(showcase, DCTerms.references, (RDFNode) null); it.hasNext(); ) {
            Statement stmt = it.next();
            assertFalse(stmt.getObject().isAnon(), "bNode should be completely removed from references");
        }
    }
}
