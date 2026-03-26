package swiss.opendata.piveau.testbench.scenarios;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import swiss.opendata.piveau.testbench.BaseSystemTest;
import swiss.opendata.piveau.testbench.Goal;
import swiss.opendata.piveau.testbench.TestContext;
import swiss.opendata.piveau.testbench.annotations.DependsOn;
import swiss.opendata.piveau.testbench.annotations.Provides;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static swiss.opendata.piveau.testbench.TestConstants.*;

public class SearchTest extends BaseSystemTest {

    @BeforeEach
    public void setupSearchClient() {
        RestAssured.baseURI = "http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080);
        RestAssured.port = getServicePort(SEARCH_SERVICE_NAME, 8080);
    }

    @Test
    @DependsOn(Goal.SIMPLE_DATASET_INDEXED)
    @Provides(Goal.SIMPLE_DATASET_SEARCH_VERIFIED)
    public void verifySimpleSearch(TestContext context) {
        String datasetId = context.get(Goal.SIMPLE_DATASET_CREATED, "id", String.class);

        System.out.println("Checking Global Search: /search?q=foo");
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            RestAssured.given().queryParam("q", "fizfaz").when().get("/search").then().statusCode(200)
                    // Piveau Search response: { "result": { "count": N, "results": [...] } }
                    .body("result.count", greaterThan(0)).body("result.results.id", hasItem(datasetId));
        });
    }

    @Test
    @DependsOn({Goal.ODSN_DATASET_INDEXED, Goal.SIMPLE_DATASET_SEARCH_VERIFIED})
    @Provides(Goal.ODSN_DATASET_FACETED_SEARCH_VERIFIED)
    public void verifyOdsnFacetedSearch(TestContext context) {
        String catalogId = context.get(Goal.ODSN_CATALOG_CREATED, "id", String.class);
        String datasetId = context.get(Goal.ODSN_DATASET_CREATED, "id", String.class);

        System.out.println("Checking ODSN Faceted Search");
        await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            RestAssured.given().queryParam("limit", 10).queryParam("page", 0).queryParam("q", "Waldbestand im Kanton ABC").queryParam("sort", "relevance").queryParam("facets", "{\"catalog\":[\"" + catalogId + "\"],\"categories\":[\"ENVI\"],\"publisher\":[\"Verein ABC\"],\"format\":[\"CSV\"],\"license\":[\"http://dcat-ap.ch/vocabulary/licenses/cc-by/4.0\"],\"keywords\":[\"forests\"]}").queryParam("filters", "dataset").when().get("/search").then().statusCode(200).body("result.count", greaterThan(0)).body("result.results.id", hasItem(datasetId));
        });
    }
}
