package swiss.opendata.piveau.testbench.scenarios.odsn;

import org.junit.jupiter.api.Test;

import swiss.opendata.piveau.testbench.BaseSystemTest;
import swiss.opendata.piveau.testbench.Goal;
import swiss.opendata.piveau.testbench.TestContext;
import swiss.opendata.piveau.testbench.annotations.DependsOn;
import swiss.opendata.piveau.testbench.annotations.Provides;
import swiss.opendata.piveau.testbench.utils.ResourceUtils;
import swiss.opendata.piveau.testbench.utils.SideEffectUtils;

import java.io.IOException;

import static swiss.opendata.piveau.testbench.TestConstants.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatasetInCatalogWithOrgaPublisherTest extends BaseSystemTest {

    @org.junit.jupiter.api.BeforeEach
    public void setupRestAssured() {
        io.restassured.RestAssured.baseURI = "http://" + getServiceHost("piveau-hub-repo", 8080);
        io.restassured.RestAssured.port = getServicePort("piveau-hub-repo", 8080);
    }

    @Test
    @DependsOn(Goal.ODSN_CATALOG_WITH_ORGA_PUBLISHER_CREATED)
    @Provides(Goal.ODSN_DATASET_IN_CATALOG_WITH_ORGA_PUBLISHER_CREATED)
    public void createDatasetInCatalogWithOrgaPublisher(TestContext context) throws IOException {
        final String catalogId = context.get(Goal.ODSN_CATALOG_WITH_ORGA_PUBLISHER_CREATED, "id", String.class);

        final String datasetId = "dataset-" + System.currentTimeMillis();

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

        io.restassured.RestAssured.given()
            .header("X-API-Key", API_KEY)
            .contentType("text/turtle")
            .body(datasetTurtle)
            .when().put("/catalogues/" + catalogId + "/datasets/origin?originalId=" + datasetId)
            .then().statusCode(is(oneOf(200, 201, 204)));

        org.awaitility.Awaitility.await().atMost(PT5S).until(() -> SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfDatasetExists));

        // Extract the minted dataset IRI from the API
        String datasetRdf = io.restassured.RestAssured.given()
            .header("X-API-Key", API_KEY)
            .accept("text/turtle")
            .when().get("/catalogues/" + catalogId + "/datasets/origin?originalId=" + datasetId)
            .then().statusCode(200)
            .extract().body().asString();
        
        String datasetIRI = SideEffectUtils.extractSubjectIri(datasetRdf, "http://www.w3.org/ns/dcat#Dataset");
        System.out.println("Minted Dataset IRI: " + datasetIRI);

        assertNotNull(datasetIRI);
        assertTrue(datasetIRI.startsWith("https://opendata.swiss/set/data/"), String.format("%s starting with https://opendata.swiss/set/data/", datasetIRI));

        context.store(Goal.ODSN_DATASET_IN_CATALOG_WITH_ORGA_PUBLISHER_CREATED, "id", datasetId);
        context.store(Goal.ODSN_DATASET_IN_CATALOG_WITH_ORGA_PUBLISHER_CREATED, "iri", datasetIRI);
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_IN_CATALOG_WITH_ORGA_PUBLISHER_CREATED)
    @Provides(Goal.ODSN_DATASET_IN_CATALOG_WITH_ORGA_PUBLISHER_INDEXED)
    public void indexDatasetInCatalogWithOrgaPublisher(TestContext context) {
        String datasetId = context.get(Goal.ODSN_DATASET_IN_CATALOG_WITH_ORGA_PUBLISHER_CREATED, "id", String.class);

        System.out.println("Checking Dataset Document after creation: /datasets/" + datasetId);

        // the response is logged, so it's available for example in target/surefire-reports/TEST-swiss.opendata.piveau.testbench.GlobalTestRunner.xml - then search for "indexDatasetInCatalogWithOrgaPublisherAfterCreation" in the logfile
        org.awaitility.Awaitility.await().atMost(PT5S).pollInterval(PT2S).untilAsserted(() -> {
            String json = io.restassured.RestAssured.given()
                .baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080))
                .when().get("/datasets/" + datasetId)
                .then().statusCode(200)
                .body("result.id", equalTo(datasetId))
                .log().body()
                .extract().body().asString();
            
            context.store(Goal.ODSN_DATASET_IN_CATALOG_WITH_ORGA_PUBLISHER_INDEXED, "json", json);
        });
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_IN_CATALOG_WITH_ORGA_PUBLISHER_INDEXED)
    public void indexDatasetInCatalogWithOrgaPublisher_catalog_publisher(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_IN_CATALOG_WITH_ORGA_PUBLISHER_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.catalog.publisher.id"), equalTo("staatskanzlei-kanton-zuerich"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.catalog.publisher.name.de"), containsString("Staatskanzlei"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.catalog.publisher.homepage"), equalTo("https://www.zh.ch/de/staatskanzlei.html"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.catalog.publisher.resource"), equalTo("https://opendata.swiss/id/organization/staatskanzlei-kanton-zuerich"));
    }
    
}
