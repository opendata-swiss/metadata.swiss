package swiss.opendata;

import io.piveau.profile.Indexer;
import io.piveau.rdf.Piveau;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

@ExtendWith(VertxExtension.class)
class ResourceToIndexTest {

    @Test
    @Timeout(timeUnit = TimeUnit.MINUTES, value = 5)
    void indexAgent(Vertx vertx, VertxTestContext testContext) {
        Buffer buffer = vertx.fileSystem().readFileBlocking("indexing/data-stadt-winterthur.ttl");

        JsonObject instructions = Indexer
                .produceIndexInstructionsFromShacl(
                        "./src/test/resources/shapes/agent-shapes.ttl",
                        Lang.TURTLE,
                        "http://data.europa.eu/r5r/Agent_Shape"
                );

        Model model = Piveau.toModel(buffer, Lang.TURTLE);

        Resource agent = ModelFactory.createDefaultModel()
                .createResource("http://xmlns.com/foaf/0.1/Agent");
        Resource provider = model.listSubjectsWithProperty(RDF.type, agent).next();

        JsonObject result = Indexer.indexingResource(
                provider,
                null,
                instructions,
                "en",
                "other",
                false,
                null,
                false
        );

        System.out.println(result.encodePrettily());

        testContext.completeNow();
    }

    @Test
    @Timeout(timeUnit = TimeUnit.MINUTES, value = 5)
    void indexCatalog(Vertx vertx, VertxTestContext testContext) {
        Buffer buffer = vertx.fileSystem().readFileBlocking("indexing/data-stadt-winterthur.ttl");

        JsonObject instructions = Indexer
                .produceIndexInstructionsFromShacl(
                        "./src/test/resources/shapes/catalog-shapes.ttl",
                        Lang.TURTLE,
                        "http://data.europa.eu/r5r/Catalog_Shape"
                );

        Model model = Piveau.toModel(buffer, Lang.TURTLE);

        Resource catalog = ModelFactory.createDefaultModel()
                .createResource("http://www.w3.org/ns/dcat#Catalog");
        Resource provider = model.listSubjectsWithProperty(RDF.type, catalog).next();

        JsonObject result = Indexer.indexingResource(
                provider,
                null,
                instructions,
                "en",
                "other",
                false,
                null,
                false
        );

        System.out.println(result.encodePrettily());

        testContext.completeNow();
    }
}
