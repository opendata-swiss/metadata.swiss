package swiss.opendata.piveau.pipe.module.importing.showcases;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShowcaseExtractorTest {

    @Test
    public void testExtractorSplitsShowcasesProperly() {
        Model model = ModelFactory.createDefaultModel();

        try (InputStream in = getClass().getResourceAsStream("/showcase-2.ttl")) {
            model.read(in, null, "TTL");
        } catch (Exception e) {
            throw new RuntimeException("Test resource could not be found", e);
        }

        ShowcaseExtractor extract = new ShowcaseExtractor();

        Resource showcaseClass = model.createResource(PayloadPatcher.SHOWCASE_TMP_URI);

        List<Model> exactractedModels = new ArrayList<>();
        List<String> uris = new ArrayList<>();

        model.listSubjectsWithProperty(RDF.type, showcaseClass).forEachRemaining(showcase -> {
            Model extracted = extract.extract(showcase, model);
            exactractedModels.add(extracted);
            uris.add(showcase.getURI());
        });

        assertEquals(2, exactractedModels.size(), "There should be two showcases extracted from the sample.");
        assertTrue(uris.contains("http://localhost:3000/showcase/mietpreisentwicklung-in-bern"));
        assertTrue(uris.contains("http://localhost:3000/showcase/weg-der-vielfalt"));

        // Assert some properties inside the extracted models
        // First model
        Model m1 = exactractedModels.get(uris.indexOf("http://localhost:3000/showcase/mietpreisentwicklung-in-bern"));
        assertTrue(m1.contains(m1.createResource("http://localhost:3000/showcase/mietpreisentwicklung-in-bern"), m1.createProperty("http://purl.org/dc/terms/title"), "Mietpreisentwicklung in Bern", "de"));

        // Second model
        Model m2 = exactractedModels.get(uris.indexOf("http://localhost:3000/showcase/weg-der-vielfalt"));
        assertTrue(m2.contains(m2.createResource("http://localhost:3000/showcase/weg-der-vielfalt"), m2.createProperty("http://purl.org/dc/terms/title"), "Weg der Vielfalt St. Gallen", "de"));
    }
}
