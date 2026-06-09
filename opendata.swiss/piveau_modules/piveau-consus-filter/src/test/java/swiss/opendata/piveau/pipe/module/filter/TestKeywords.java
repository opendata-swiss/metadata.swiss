package swiss.opendata.piveau.pipe.module.filter;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCAT;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestKeywords {
    @Test
    void testCheckKeywords() {

        Model model = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
        Resource dataset = model.createResource("http://example.com/dataset");
        model.add(dataset, DCAT.keyword, model.createResource("http://example.com/keyword1"));
        model.add(dataset, DCAT.keyword, model.createResource("https://register.ld.admin.ch/termdat/92053"));

        MainVerticle.checkKeywords(model, dataset);

        // assert that the model now contains the expected keywords besides the original ones
        assertTrue(model.contains(dataset, DCAT.keyword, model.createResource("http://example.com/keyword1")));
        assertTrue(model.contains(dataset, DCAT.keyword, model.createResource("https://register.ld.admin.ch/termdat/92053")));
        assertTrue(model.contains(dataset, DCAT.keyword, model.createLiteral("Züchten", "de")));
        assertTrue(model.contains(dataset, DCAT.keyword, model.createLiteral("élevage", "fr")));
        assertTrue(model.contains(dataset, DCAT.keyword, model.createLiteral("allevamento", "it")));
    }
}
