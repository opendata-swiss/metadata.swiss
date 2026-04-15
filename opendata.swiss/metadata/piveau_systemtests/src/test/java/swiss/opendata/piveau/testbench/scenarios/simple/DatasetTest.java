package swiss.opendata.piveau.testbench.scenarios.simple;

import org.junit.jupiter.api.Disabled;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class DatasetTest extends BaseSystemTest {

    @org.junit.jupiter.api.BeforeEach
    public void setupRestAssured() {
        io.restassured.RestAssured.baseURI = "http://" + getServiceHost("piveau-hub-repo", 8080);
        io.restassured.RestAssured.port = getServicePort("piveau-hub-repo", 8080);
    }

    @Test
    @DependsOn(Goal.SIMPLE_CATALOG_CREATED)
    @Provides(Goal.SIMPLE_DATASET_CREATED)
    public void createDataset(TestContext context) throws IOException {
        final String catalogId = context.get(Goal.SIMPLE_CATALOG_CREATED, "id", String.class);
        final String catalogIRI = context.get(Goal.SIMPLE_CATALOG_CREATED, "iri", String.class);

        final String datasetId = "test-dataset-" + System.currentTimeMillis();

        String datasetTurtle = ResourceUtils.loadTurtle("/dataset.ttl", datasetId);

        String askIfDatasetExists = """
                %s
                ASK {
                    GRAPH ?g {
                        ?s a dcat:Dataset ;
                           dct:identifier "%s" .
                    }
                }
                """.formatted(PREFIXES, datasetId);

        assertFalse(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfDatasetExists));

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(datasetTurtle).when().put("/catalogues/" + catalogId + "/datasets/origin?originalId=" + datasetId).then().statusCode(is(oneOf(200, 201, 204)));

        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfDatasetExists));

        // Extract the minted dataset IRI from the API
        String datasetRdf = io.restassured.RestAssured.given().header("X-API-Key", API_KEY).accept("text/turtle").when().get("/catalogues/" + catalogId + "/datasets/origin?originalId=" + datasetId).then().statusCode(200).extract().body().asString();
        String datasetIRI = SideEffectUtils.extractSubjectIri(datasetRdf, "http://www.w3.org/ns/dcat#Dataset");
        System.out.println("Minted Dataset IRI: " + datasetIRI);

        assertNotNull(datasetIRI);

        String askIfCatalogAndDatasetAreLinked = """
                %s
                ASK {
                    GRAPH ?g {
                        <%s> dcat:dataset <%s> .
                    }
                }
                """.formatted(PREFIXES, catalogIRI, datasetIRI);

        assertTrue(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfCatalogAndDatasetAreLinked));

        context.store(Goal.SIMPLE_DATASET_CREATED, "id", datasetId);
        context.store(Goal.SIMPLE_DATASET_CREATED, "iri", datasetIRI);
    }

    @Test
    @DependsOn(Goal.SIMPLE_DATASET_CREATED)
    @Provides(Goal.SIMPLE_DATASET_INDEXED)
    public void indexDatasetAfterCreation(TestContext context) {
        String datasetId = context.get(Goal.SIMPLE_DATASET_CREATED, "id", String.class);

        System.out.println("Checking Dataset Document after creation: /datasets/" + datasetId);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/datasets/" + datasetId).then().statusCode(200).body("result.id", equalTo(datasetId));
        });
    }

    @Test
    @Disabled
    @DependsOn(Goal.SIMPLE_CATALOG_CREATED)
    public void createDataset_curl(TestContext context) throws IOException, InterruptedException {

        fail("on purpose");

        String catalogId = context.get(Goal.SIMPLE_CATALOG_CREATED, "id", String.class);

        String datasetId = "test-dataset-" + System.currentTimeMillis();

        String datasetTurtle = ResourceUtils.loadTurtle("/dataset.ttl", datasetId);

        String askIfDatasetExists = """
                %s
                ASK {
                    GRAPH ?g {
                        ?s a dcat:Dataset ;
                           dct:identifier "%s" .
                    }
                }
                """.formatted(PREFIXES, datasetId);

        assertFalse(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfDatasetExists));

        // DEBUG: Run curl to see raw response headers
        String curlUrl = io.restassured.RestAssured.baseURI + ":" + io.restassured.RestAssured.port + "/catalogues/" + catalogId + "/datasets/origin?originalId=" + datasetId;
        System.out.println("DEBUG: curl URL: " + curlUrl);

        java.io.File tmpBody = java.io.File.createTempFile("dataset", ".ttl");
        java.nio.file.Files.writeString(tmpBody.toPath(), datasetTurtle);

        ProcessBuilder pb = new ProcessBuilder(
                "curl", "-vi", "-X", "PUT", "-H", "X-API-Key: " + API_KEY, "-H", "Content-Type: text/turtle", "--data-binary", "@" + tmpBody.getAbsolutePath(), curlUrl
        );
        System.out.println("DEBUG: curl command: " + String.join(" ", pb.command()));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String curlOutput = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        tmpBody.delete();
        System.out.println("DEBUG: Curl output:\n" + curlOutput);

        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfDatasetExists));
    }


    @Test
    @DependsOn(Goal.SIMPLE_DATASET_SEARCH_VERIFIED)
    @Provides(Goal.SIMPLE_DATASET_UPDATED)
    public void updateDataset(TestContext context) throws IOException {
        String catalogId = context.get(Goal.SIMPLE_CATALOG_CREATED, "id", String.class);
        String datasetId = context.get(Goal.SIMPLE_DATASET_CREATED, "id", String.class);
        String datasetIRI = context.get(Goal.SIMPLE_DATASET_CREATED, "iri", String.class);

        String datasetUpdateTurtle = ResourceUtils.loadTurtle("/dataset_update.ttl", datasetId);

        String askIfDatasetExists = """
                %s
                ASK {
                    GRAPH ?g {
                        <%s> a dcat:Dataset ;
                           dct:identifier "%s" ;
                           dct:title "Updated title" .
                    }
                }
                """.formatted(PREFIXES, datasetIRI, datasetId);

        assertFalse(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfDatasetExists));

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(datasetUpdateTurtle).when().put("/catalogues/" + catalogId + "/datasets/origin?originalId=" + datasetId).then().statusCode(is(oneOf(200, 204)));

        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfDatasetExists));
    }

    @Test
    @DependsOn(Goal.SIMPLE_DATASET_UPDATED)
    @Provides(Goal.SIMPLE_DATASET_INDEX_UPDATED)
    public void indexDatasetAfterUpdate(TestContext context) {
        String datasetId = context.get(Goal.SIMPLE_DATASET_CREATED, "id", String.class);

        System.out.println("Checking Dataset Document after update: /datasets/" + datasetId);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/datasets/" + datasetId).then().statusCode(200).body("result.id", equalTo(datasetId)).body("result.title", hasEntry(is(oneOf("en", "de", "fr", "it", "rm")), equalTo("Updated title")));
        });
    }

    @Test
    @DependsOn(Goal.SIMPLE_DATASET_INDEX_UPDATED)
    @Provides(Goal.SIMPLE_DATASET_DELETED)
    public void deleteDataset(TestContext context) {
        String catalogId = context.get(Goal.SIMPLE_CATALOG_CREATED, "id", String.class);
        String datasetId = context.get(Goal.SIMPLE_DATASET_CREATED, "id", String.class);
        String datasetIRI = context.get(Goal.SIMPLE_DATASET_CREATED, "iri", String.class);

        String askIfDatasetExists = """
                %s
                ASK {
                    GRAPH ?g {
                        <%s> a dcat:Dataset ;
                           dct:identifier "%s" .
                    }
                }
                """.formatted(PREFIXES, datasetIRI, datasetId);

        assertTrue(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfDatasetExists));

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).when().delete("/catalogues/" + catalogId + "/datasets/origin?originalId=" + datasetId).then().statusCode(is(oneOf(200, 204)));

        // Verify Side Effect: SPARQL
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> !SideEffectUtils.checkSparqlAsk(
                getSparqlEndpoint(), askIfDatasetExists
        ));
    }

    @Test
    @DependsOn(Goal.SIMPLE_DATASET_DELETED)
    @Provides(Goal.SIMPLE_DATASET_INDEX_DELETED)
    public void deleteIndexAfterDatasetDeletion(TestContext context) {
        String datasetId = context.get(Goal.SIMPLE_DATASET_CREATED, "id", String.class);

        System.out.println("Checking Dataset Document after deletion: /datasets/" + datasetId);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/datasets/" + datasetId).then().statusCode(404);
        });
    }
}
