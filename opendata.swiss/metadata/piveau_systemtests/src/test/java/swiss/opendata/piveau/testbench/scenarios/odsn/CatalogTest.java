package swiss.opendata.piveau.testbench.scenarios.odsn;

import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.junit.jupiter.api.Test;
import swiss.opendata.piveau.testbench.BaseSystemTest;
import swiss.opendata.piveau.testbench.Goal;
import swiss.opendata.piveau.testbench.TestContext;
import swiss.opendata.piveau.testbench.annotations.DependsOn;
import swiss.opendata.piveau.testbench.annotations.Provides;
import swiss.opendata.piveau.testbench.utils.ResourceUtils;
import swiss.opendata.piveau.testbench.utils.SideEffectUtils;

import java.io.IOException;
import java.time.Duration;

import static swiss.opendata.piveau.testbench.TestConstants.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CatalogTest extends BaseSystemTest {

    @org.junit.jupiter.api.BeforeEach
    public void setupRestAssured() {
        io.restassured.RestAssured.baseURI = "http://" + getServiceHost("piveau-hub-repo", 8080);
        io.restassured.RestAssured.port = getServicePort("piveau-hub-repo", 8080);
    }

    @Test
    @DependsOn(Goal.HUB_READY)
    @Provides(Goal.ODSN_CATALOG_CREATED)
    public void createCatalog(TestContext context) throws IOException {
        final long timestamp = System.currentTimeMillis();
        final String catalogId = "test-catalog-" + timestamp;
        final String catalogTitle = "Test Catalog " + timestamp;

        String catalogTurtle = ResourceUtils.loadTurtle("/catalog-odsn-abc.ttl", catalogTitle);

        String askIfCatalogExists = """
                %s
                ASK {
                    GRAPH ?g {
                        ?catalog a dcat:Catalog ;
                           dct:title "%s"@en .
                    }
                }
                """.formatted(PREFIXES, catalogTitle);

        assertFalse(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfCatalogExists));

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(catalogTurtle).when().put("/catalogues/" + catalogId).then().statusCode(is(oneOf(200, 201, 204)));

        // Verify Side Effect: SPARQL
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> SideEffectUtils.checkSparqlAsk(
                getSparqlEndpoint(), askIfCatalogExists
        ));

        // Extract the minted catalogue IRI from the API
        String catalogRdf = io.restassured.RestAssured.given().accept("text/turtle").when().get("/catalogues/" + catalogId).then().statusCode(200).extract().body().asString();
        String catalogIRI = SideEffectUtils.extractSubjectIri(catalogRdf, DCAT.CATALOG.stringValue());
        System.out.println("Minted Catalogue IRI: " + catalogIRI);

        assertNotNull(catalogIRI);
        assertTrue(catalogIRI.startsWith("https://opendata.swiss/id/catalogue/"));

        context.store(Goal.ODSN_CATALOG_CREATED, "id", catalogId);
        context.store(Goal.ODSN_CATALOG_CREATED, "iri", catalogIRI);
        context.store(Goal.ODSN_CATALOG_CREATED, "title", catalogTitle);
    }

    @Test
    @DependsOn(Goal.ODSN_CATALOG_CREATED)
    @Provides(Goal.ODSN_CATALOG_INDEXED)
    public void indexCatalogAfterCreation(TestContext context) {
        String catalogId = context.get(Goal.ODSN_CATALOG_CREATED, "id", String.class);
        String catalogTitle = context.get(Goal.ODSN_CATALOG_CREATED, "title", String.class);

        System.out.println("Checking Catalog Document after creation: /catalogues/" + catalogId);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/catalogues/" + catalogId).then().statusCode(200).body("result.id", equalTo(catalogId)).body("result.title", hasEntry(is(oneOf("en", "de", "fr", "it", "rm")), equalTo(catalogTitle)));
        });
    }

    @Test
    @DependsOn(Goal.ODSN_CATALOG_INDEXED)
    @Provides(Goal.ODSN_CATALOG_UPDATED)
    public void updateCatalog(TestContext context) throws IOException {
        String catalogId = context.get(Goal.ODSN_CATALOG_CREATED, "id", String.class);
        String oldTitle = context.get(Goal.ODSN_CATALOG_CREATED, "title", String.class);
        String newTitle = oldTitle + " Updated";

        String catalogTurtle = ResourceUtils.loadTurtle("/catalog-odsn-abc.ttl", newTitle);

        String askIfCatalogUpdated = """
                %s
                ASK {
                    GRAPH ?g {
                        ?catalog a dcat:Catalog ;
                           dct:title "%s"@en .
                    }
                }
                """.formatted(PREFIXES, newTitle);

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(catalogTurtle).when().put("/catalogues/" + catalogId).then().statusCode(is(oneOf(200, 204)));

        // Verify Side Effect: SPARQL
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> SideEffectUtils.checkSparqlAsk(
                getSparqlEndpoint(), askIfCatalogUpdated
        ));

        context.store(Goal.ODSN_CATALOG_UPDATED, "title", newTitle);
    }

    @Test
    @DependsOn(Goal.ODSN_CATALOG_UPDATED)
    @Provides(Goal.ODSN_CATALOG_INDEX_UPDATED)
    public void indexCatalogAfterUpdate(TestContext context) {
        String catalogId = context.get(Goal.ODSN_CATALOG_CREATED, "id", String.class);
        String updatedTitle = context.get(Goal.ODSN_CATALOG_UPDATED, "title", String.class);

        System.out.println("Checking Catalog Document after update: /catalogues/" + catalogId);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/catalogues/" + catalogId).then().statusCode(200).body("result.id", equalTo(catalogId)).body("result.title", hasEntry(is(oneOf("en", "de", "fr", "it", "rm")), equalTo(updatedTitle)));
        });
    }

    @Test
    @DependsOn({Goal.ODSN_CATALOG_INDEX_UPDATED, Goal.ODSN_DATASET_INDEX_DELETED})
    @Provides(Goal.ODSN_CATALOG_DELETED)
    public void deleteCatalog(TestContext context) {
        String catalogId = context.get(Goal.ODSN_CATALOG_CREATED, "id", String.class);
        String title = context.get(Goal.ODSN_CATALOG_UPDATED, "title", String.class);

        String askIfCatalogExists = """
                %s
                ASK {
                    GRAPH ?g {
                        ?catalog a dcat:Catalog ;
                           dct:title "%s"@en .
                    }
                }
                """.formatted(PREFIXES, title);

        assertTrue(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfCatalogExists));

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).when().delete("/catalogues/" + catalogId).then().statusCode(is(oneOf(200, 204)));

        // Verify Side Effect: SPARQL
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> !SideEffectUtils.checkSparqlAsk(
                getSparqlEndpoint(), askIfCatalogExists
        ));
    }

    @Test
    @DependsOn(Goal.ODSN_CATALOG_DELETED)
    @Provides(Goal.ODSN_CATALOG_INDEX_DELETED)
    public void deleteIndexAfterCatalogDeletion(TestContext context) {
        String catalogId = context.get(Goal.ODSN_CATALOG_CREATED, "id", String.class);

        System.out.println("Checking Catalog Document after deletion: /catalogues/" + catalogId);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/catalogues/" + catalogId).then().statusCode(404);
        });
    }
}
