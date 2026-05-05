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
import java.time.Duration;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static swiss.opendata.piveau.testbench.TestConstants.*;

public class DatasetVariaTest extends BaseSystemTest {

    @org.junit.jupiter.api.BeforeEach
    public void setupRestAssured() {
        io.restassured.RestAssured.baseURI = "http://" + getServiceHost("piveau-hub-repo", 8080);
        io.restassured.RestAssured.port = getServicePort("piveau-hub-repo", 8080);
    }

    @Test
    @DependsOn(Goal.ODSN_CATALOG_VARIA_CREATED)
    @Provides(Goal.ODSN_DATASET_BFS_36503351_CREATED)
    public void createDataset_bfs_36503351(TestContext context) throws IOException {
        final String catalogId = context.get(Goal.ODSN_CATALOG_VARIA_CREATED, "id", String.class);

        final String datasetId = "bfs-36503351-" + System.currentTimeMillis();

        String datasetTurtle = ResourceUtils.loadTurtle("/dataset-bfs-36503351.ttl", datasetId);

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
        assertTrue(datasetIRI.startsWith("https://opendata.swiss/set/data/"), String.format("%s starting with https://opendata.swiss/set/data/", datasetIRI));

        context.store(Goal.ODSN_DATASET_BFS_36503351_CREATED, "id", datasetId);
        context.store(Goal.ODSN_DATASET_BFS_36503351_CREATED, "iri", datasetIRI);
    }

/*
https://gitlab.zazuko.tools/opendata.swiss/new-opendata.swiss/-/work_items/245
bfs-dcat-harvester - https://dam-api.bfs.admin.ch/hub/api/ogd/harvest?limit=50
error is caused by _:myDataset dcterms:spatial "Politische Gemeinden", "Gemeinden" .

2026-05-04 14:50:49,003 [vert.x-worker-thread-8] ERROR GeoParser - Unknown data type: http://www.w3.org/2001/XMLSchema#string
2026-05-04 14:50:49,007 [vert.x-worker-thread-8] ERROR piveau.hub - [repo] [36503351@bundesamt-fur-statistik-bfs] Indexing failure: {}
2026-05-04 14:50:49,375 [vert.x-worker-thread-2] ERROR piveau.hub.search - [DatasetsService] [36503351-bundesamt-fur-statistik-bfs] Patch failed: dataset 36503351-bundesamt-fur-statistik-bfs not found
 */
    @Test
    @DependsOn(Goal.ODSN_DATASET_BFS_36503351_CREATED)
    @Provides(Goal.ODSN_DATASET_BFS_36503351_INDEXED)
    public void indexDataset_bfs_36503351_AfterCreation(TestContext context) {
        String datasetId = context.get(Goal.ODSN_DATASET_BFS_36503351_CREATED, "id", String.class);

        System.out.println("Checking Dataset Document after creation: /datasets/" + datasetId);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            io.restassured.RestAssured.given()
                    .baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080))
                    .when().get("/datasets/" + datasetId).then().statusCode(200)
                    .body("result.id", equalTo(datasetId));
        });
    }
}