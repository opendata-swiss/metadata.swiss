package swiss.opendata.piveau.testbench.scenarios;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import swiss.opendata.piveau.testbench.BaseSystemTest;
import swiss.opendata.piveau.testbench.Goal;
import swiss.opendata.piveau.testbench.TestContext;
import swiss.opendata.piveau.testbench.annotations.DependsOn;
import swiss.opendata.piveau.testbench.annotations.Provides;

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
    public void datasetSearch(TestContext context) {
        String datasetId = context.get(Goal.SIMPLE_DATASET_CREATED, "id", String.class);

        String query = "fizfaz";
        String filters = "dataset";

        System.out.println("Checking dataset search");
        System.out.println("GET /search with params: "
            + "q=" + query
            + ", filters=" + filters);

        await().atMost(PT5S).pollInterval(PT2S).untilAsserted(() -> {
            RestAssured.given()
                .filter((requestSpec, responseSpec, ctx) -> {
                    System.out.println("RestAssured URI: " + requestSpec.getURI());
                    return ctx.next(requestSpec, responseSpec);
                })
                .queryParam("q", query)
                .queryParam("filters", filters)
                .when().get("/search")
                .then().statusCode(200)

                // Piveau Search response: { "result": { "count": N, "results": [...] } }
                .log().body()
                .body("result.count", greaterThan(0))
                .body("result.results.id", hasItem(datasetId));
        });
    }

    @Test
    @DependsOn(Goal.SIMPLE_ORGANIZATION_INDEXED)
    @Provides(Goal.SIMPLE_ORGANIZATION_SEARCH_VERIFIED)
    public void organizationSearch(TestContext context) {
        String orgId = context.get(Goal.SIMPLE_ORGANIZATION_CREATED, "id", String.class);

        String query = "orgaorga";
        String filters = "organization";

        System.out.println("Checking organization search");
        System.out.println("GET /search with params: "
            + "q=" + query
            + ", filters=" + filters);

        await().atMost(PT5S).pollInterval(PT2S).untilAsserted(() -> {
            RestAssured.given()
                .filter((requestSpec, responseSpec, ctx) -> {
                    System.out.println("RestAssured URI: " + requestSpec.getURI());
                    return ctx.next(requestSpec, responseSpec);
                })
                .queryParam("q", query)
                .queryParam("filters", filters)
                .when().get("/search")
                .then().statusCode(200)

                // Piveau Search response: { "result": { "count": N, "results": [...] } }
                .log().body()
                .body("result.count", greaterThan(0))
                .body("result.results.id", hasItem(orgId));
        });
    }

    @Test
    @DependsOn({Goal.ODSN_ORGANIZATION_HIERARCHY_INDEXED, Goal.SIMPLE_ORGANIZATION_SEARCH_VERIFIED})
    @Provides(Goal.ODSN_ORGANIZATION_FACETED_SEARCH_VERIFIED)
    public void facetedOrganizationSearch(TestContext context) {
        String orgId = context.get(Goal.ODSN_ORGANIZATION_HIERARCHY_CREATED, "idLevel2", String.class);

        String query = "Dialoggruppen";
        String filters = "organization";

        String facets = """
            {
              "classification": ["0221"]
            }
            """;

        System.out.println("Checking faceted organization search");
        System.out.println("GET /search with params: "
            + "q=" + query
            + ", filters=" + filters
            + ", facets=" + facets);

        await().atMost(PT5S).pollInterval(PT2S).untilAsserted(() -> {
            RestAssured.given()
                .filter((requestSpec, responseSpec, ctx) -> {
                    System.out.println("RestAssured URI: " + requestSpec.getURI());
                    return ctx.next(requestSpec, responseSpec);
                })
                .queryParam("q", query)
                .queryParam("filters", filters)
                .queryParam("facets", facets)
                .when().get("/search")
                .then().statusCode(200)

                // Piveau Search response: { "result": { "count": N, "results": [...] } }
                .log().body()
                .body("result.count", greaterThan(0))
                .body("result.results.id", hasItem(orgId))

                .body("result.facets.find { it.id == 'classification' }.items.id", hasItem("0221"))
                .body("result.facets.find { it.id == 'classification' }.items.title.de", hasItem("Verwaltungseinheit des Kantons"));
        });
    }

    @Test
    @DependsOn({Goal.ODSN_DATASET_INDEXED, Goal.SIMPLE_DATASET_SEARCH_VERIFIED})
    @Provides(Goal.ODSN_DATASET_FACETED_SEARCH_VERIFIED)
    public void facetedDatasetSearch(TestContext context) {
        String catalogId = context.get(Goal.ODSN_CATALOG_CREATED, "id", String.class);
        String datasetId = context.get(Goal.ODSN_DATASET_CREATED, "id", String.class);
        
        int limit = 10;
        int page = 0;
        String query = "Waldbestand im Kanton ABC";
        String sort = "relevance";
        String filters = "dataset";

        // TODO: select facet - "publisher": ["Verein ABC"],
        // Catalogues, Categories, Formats
        // Keywords, Licenses, Publisher
        String facets = """
            {
              "catalog": ["%s"],
              "categories": ["ENVI"],
              
              "format": ["CSV"],
              "license": ["http://dcat-ap.ch/vocabulary/licenses/cc-by/4.0"],
              "keywords": ["forests"]
            }
            """.formatted(catalogId);

        System.out.println("Checking faceted dataset search");
        System.out.println("GET /search with params: "
            + "limit=" + limit
            + ", page=" + page
            + ", q=" + query
            + ", sort=" + sort
            + ", filters=" + filters
            + ", facets=" + facets);

        await().atMost(PT5S).pollInterval(PT2S).untilAsserted(() -> {
            RestAssured.given()
                .filter((requestSpec, responseSpec, ctx) -> {
                    System.out.println("RestAssured URI: " + requestSpec.getURI());
                    return ctx.next(requestSpec, responseSpec);
                })
                .queryParam("limit", limit)
                .queryParam("page", page)
                .queryParam("q", query)
                .queryParam("sort", sort)
                .queryParam("facets", facets)
                .queryParam("filters", filters)
                .when().get("/search")
                .then().statusCode(200)
                .log().body()
                .body("result.count", greaterThan(0))
                .body("result.results.id", hasItem(datasetId))
                
                .body("result.facets.find { it.id == 'catalog' }.items.id", hasItem(catalogId))
                // .body("result.facets.find { it.id == 'catalog' }.items.title", hasItem("Offene Daten ABC"))
                // TODO: should the catalog title be localized? .. it seems that not always the same language gets picked !?
                
                .body("result.facets.find { it.id == 'categories' }.items.id", hasItem("ENVI"))
                .body("result.facets.find { it.id == 'categories' }.items.title.it", hasItem("Ambiente"))

                // TODO: 'publisher' facet is ATM not populated -- https://github.com/opendata-swiss/metadata.swiss/pull/326#issuecomment-5131347111
                .body("result.facets.find { it.id == 'publisher' }.items.id", hasItem("some id"))
                .body("result.facets.find { it.id == 'publisher' }.items.title.de", hasItem("Verein ABC"))

                .body("result.facets.find { it.id == 'keywords' }.items.id", hasItem("forests"))
                .body("result.facets.find { it.id == 'keywords' }.items.title", hasItem("forests"))
                
                .body("result.facets.find { it.id == 'format' }.items.id", hasItem("CSV"))
                .body("result.facets.find { it.id == 'format' }.items.title", hasItem("CSV"))

                .body("result.facets.find { it.id == 'license' }.items.id", hasItem("http://dcat-ap.ch/vocabulary/licenses/cc-by/4.0"));
        });
    }

    @Test
    // @DependsOn({Goal.ODSN_DATASET_IN_CATALOG_WITH_ORGA_PUBLISHER_INDEXED, Goal.ODSN_DATASET_FACETED_SEARCH_VERIFIED})
    @DependsOn({Goal.ODSN_DATASET_IN_CATALOG_WITH_ORGA_PUBLISHER_INDEXED})
    @Provides(Goal.ODSN_DATASET_FACETED_SEARCH_WITH_ORGA_PUBLISHER_VERIFIED)
    public void facetedDatasetSearchWithOrgaPublisher(TestContext context) {
        String catalogId = context.get(Goal.ODSN_CATALOG_WITH_ORGA_PUBLISHER_CREATED, "id", String.class);
        String datasetId = context.get(Goal.ODSN_DATASET_IN_CATALOG_WITH_ORGA_PUBLISHER_CREATED, "id", String.class);
        
        int limit = 100;
        int page = 0;
        String filters = "dataset";

        String facets = """
            {
              "catalog": ["%s"]
              "organization": ["staatskanzlei-kanton-zuerich"],
              "classification": ["0221"]
            }
            """.formatted(catalogId);

        System.out.println("Checking faceted dataset search - with organization as publisher");
        System.out.println("GET /search with params: "
            + "limit=" + limit
            + ", page=" + page
            + ", filters=" + filters
            + ", facets=" + facets);

        await().atMost(PT5S).pollInterval(PT2S).untilAsserted(() -> {
            RestAssured.given()
                .filter((requestSpec, responseSpec, ctx) -> {
                    System.out.println("RestAssured URI: " + requestSpec.getURI());
                    return ctx.next(requestSpec, responseSpec);
                })
                .queryParam("limit", limit)
                .queryParam("page", page)
                .queryParam("facets", facets)
                .queryParam("filters", filters)
                .when().get("/search")
                .then().statusCode(200)
                .log().body()
                .body("result.count", greaterThan(0))
                .body("result.results.id", hasItem(datasetId))
                
                .body("result.facets.find { it.id == 'catalog' }.items.id", hasItem(catalogId))
                
                .body("result.facets.find { it.id == 'organization' }.items.id", hasItem("staatskanzlei-kanton-zuerich"))
                .body("result.facets.find { it.id == 'organization' }.items.title.de", hasItems(startsWith("Staatskanzlei Kanton Z")))

                .body("result.facets.find { it.id == 'classification' }.items.id", hasItem("0221"))
                .body("result.facets.find { it.id == 'classification' }.items.title.de", hasItem("Verwaltungseinheit des Kantons"));
        });
    }

    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_INDEXED)
    @Provides(Goal.ODSN_SHOWCASE_SEARCH_VERIFIED)
    public void showcaseSearch(TestContext context) {
        String showcaseId = context.get(Goal.ODSN_SHOWCASE_CREATED, "id", String.class);

        String query = "mietpreisentwicklung";
        String filters = "resource";
        String resource = "showcase";

        System.out.println("Checking showcase search");
        System.out.println("GET /search with params: "
            + "q=" + query
            + ", filters=" + filters
            + ", resource=" + resource);

        await().atMost(PT5S).pollInterval(PT2S).untilAsserted(() -> {
            RestAssured.given()
                .filter((requestSpec, responseSpec, ctx) -> {
                    System.out.println("RestAssured URI: " + requestSpec.getURI());
                    return ctx.next(requestSpec, responseSpec);
                })
                .queryParam("q", query)
                .queryParam("filters", filters)
                .queryParam("resource", resource)
                .when().get("/search")
                .then().statusCode(200)

                // Piveau Search response: { "result": { "count": N, "results": [...] } }
                .log().body()
                .body("result.count", greaterThan(0))
                .body("result.results.id", hasItem(showcaseId));
        });
    }

    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_SEARCH_VERIFIED)
    @Provides(Goal.ODSN_SHOWCASE_FACETED_SEARCH_VERIFIED)
    public void facetedShowcaseSearch(TestContext context) {
        String showcaseId = context.get(Goal.ODSN_SHOWCASE_CREATED, "id", String.class);

        String query = "mietpreisentwicklung";
        String filters = "resource";
        String resource = "showcase";

        String facets = """
            {
              "categories": ["SOCI"],
              "type": ["application"],
              "keywords": ["bern"]
            }
            """;

        System.out.println("Checking faceted showcase search");
        System.out.println("GET /search with params: "
            + "q=" + query
            + ", filters=" + filters
            + ", resource=" + resource
            + ", facets=" + facets);

        await().atMost(PT5S).pollInterval(PT2S).untilAsserted(() -> {
            RestAssured.given()
                .filter((requestSpec, responseSpec, ctx) -> {
                    System.out.println("RestAssured URI: " + requestSpec.getURI());
                    return ctx.next(requestSpec, responseSpec);
                })
                .queryParam("q", query)
                .queryParam("filters", filters)
                .queryParam("resource", resource)
                .queryParam("facets", facets)
                .when().get("/search")
                .then().statusCode(200)

                // Piveau Search response: { "result": { "count": N, "results": [...] } }
                .log().body()
                .body("result.count", greaterThan(0))
                .body("result.results.id", hasItem(showcaseId))
                .body("result.facets.find { it.id == 'type' }.items.id", hasItem("application"))
                .body("result.facets.find { it.id == 'type' }.items.title.it", hasItem("Applicazione"))
                
                .body("result.facets.find { it.id == 'categories' }.items.id", hasItem("SOCI"))
                .body("result.facets.find { it.id == 'categories' }.items.title.en", hasItem("Population and society"))
                
                .body("result.facets.find { it.id == 'keywords' }.items.id", hasItem("bern"))
                .body("result.facets.find { it.id == 'keywords' }.items.title", hasItem("bern"));
        });
    }
}
