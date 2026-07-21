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

import static swiss.opendata.piveau.testbench.TestConstants.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShowcaseTest extends BaseSystemTest {

    @org.junit.jupiter.api.BeforeEach
    public void setupRestAssured() {
        io.restassured.RestAssured.baseURI = "http://" + getServiceHost("piveau-hub-repo", 8080);
        io.restassured.RestAssured.port = getServicePort("piveau-hub-repo", 8080);
    }

    @Test
    @DependsOn(Goal.ODSN_CATALOG_SHOWCASES_CREATED)
    @Provides(Goal.ODSN_SHOWCASE_CREATED)
    public void createShowcase(TestContext context) throws IOException {
        final String catalogId = context.get(Goal.ODSN_CATALOG_SHOWCASES_CREATED, "id", String.class);

        final String showcaseId = "showcase-" + System.currentTimeMillis();

        String showcaseTurtle = ResourceUtils.loadTurtle("/showcase-mietpreisentwicklung.ttl", showcaseId, "Mietpreisentwicklung in Bern");

        String askIfShowcaseExists = """
                %s
                ASK {
                    GRAPH ?g {
                        ?s a <https://example.org/Showcase> ;
                           dct:identifier "%s" .
                    }
                }
                """.formatted(PREFIXES, showcaseId);

        assertFalse(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfShowcaseExists));

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(showcaseTurtle).when().put("/customresources/showcase?catalogId=" + catalogId + "&id=" + showcaseId).then().statusCode(is(oneOf(200, 201, 204)));

        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfShowcaseExists));

        // Extract the minted showcase IRI from the API
        String showcaseRdf = io.restassured.RestAssured.given().header("X-API-Key", API_KEY).accept("text/turtle").when().get("/customresources/showcase/origin?catalogId=" + catalogId + "&id=" + showcaseId).then().statusCode(200).extract().body().asString();
        String showcaseIRI = SideEffectUtils.extractSubjectIri(showcaseRdf, "https://example.org/Showcase");
        System.out.println("Minted Showcase IRI: " + showcaseIRI);
        
        assertNotNull(showcaseIRI);
        assertTrue(showcaseIRI.startsWith("https://opendata.swiss/set/resource/showcase/"), String.format("%s starting with https://opendata.swiss/set/resource/showcase/", showcaseIRI));

        context.store(Goal.ODSN_SHOWCASE_CREATED, "id", showcaseId);
        context.store(Goal.ODSN_SHOWCASE_CREATED, "iri", showcaseIRI);
    }

    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_CREATED)
    @Provides(Goal.ODSN_SHOWCASE_INDEXED)
    public void indexShowcaseAfterCreation(TestContext context) {
        String showcaseId = context.get(Goal.ODSN_SHOWCASE_CREATED, "id", String.class);

        System.out.println("Checking Showcase Document after creation: /resources/showcase/" + showcaseId);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/resources/showcase/" + showcaseId).then().statusCode(200).body("result.id", equalTo(showcaseId)).body("result.title.de", equalTo("Mietpreisentwicklung in Bern"));
        });

        // the response is logged, so it's available for example in target/surefire-reports/TEST-swiss.opendata.piveau.testbench.GlobalTestRunner.xml - then search for "indexShowcaseAfterCreation" in the logfile
        String json = io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/resources/showcase/" + showcaseId).then().log().body().statusCode(200).extract().body().asString();
        context.store(Goal.ODSN_SHOWCASE_INDEXED, "json", json);
    }

    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_INDEXED)
    public void indexShowcase_issued(TestContext context) {
        String json = context.get(Goal.ODSN_SHOWCASE_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.issued"), notNullValue());
    }

    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_INDEXED)
    public void indexShowcase_modified(TestContext context) {
        String json = context.get(Goal.ODSN_SHOWCASE_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.modified"), notNullValue());
    }

    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_INDEXED)
    public void indexShowcase_abstract(TestContext context) {
        String json = context.get(Goal.ODSN_SHOWCASE_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.abstract.de"), equalTo("Dieses interaktive Dashboard zeigt..."));
    }
    
    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_INDEXED)
    public void indexShowcase_theme(TestContext context) {
        String json = context.get(Goal.ODSN_SHOWCASE_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.categories"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.categories[0].resource"), equalTo("http://publications.europa.eu/resource/authority/data-theme/SOCI"));
    }
    
    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_INDEXED)
    public void indexShowcase_references(TestContext context) {
        String json = context.get(Goal.ODSN_SHOWCASE_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.references"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.references"), hasItem("https://opendata.swiss/set/data/523-staatskanzlei-kanton-zuerich"));
    }

    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_INDEXED)
    public void indexShowcase_image(TestContext context) {
        String json = context.get(Goal.ODSN_SHOWCASE_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.image"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.image"), hasItem("https://repository-images.githubusercontent.com/788945570/img.jpg"));
    }

    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_INDEXED)
    public void indexShowcase_keyword(TestContext context) {
        String json = context.get(Goal.ODSN_SHOWCASE_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.keywords"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.keywords.id"), hasItem("bern"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.keywords.language"), hasItem("en"));
    }

    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_INDEXED)
    public void indexShowcase_type(TestContext context) {
        String json = context.get(Goal.ODSN_SHOWCASE_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.type"), equalTo("https://opendata.swiss/vocabulary/showcase-type/application"));
    }

    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_SEARCH_VERIFIED)
    @Provides(Goal.ODSN_SHOWCASE_UPDATED)
    public void updateShowcase(TestContext context) throws IOException {
        String catalogId = context.get(Goal.ODSN_CATALOG_SHOWCASES_CREATED, "id", String.class);
        String showcaseId = context.get(Goal.ODSN_SHOWCASE_CREATED, "id", String.class);
        String showcaseIRI = context.get(Goal.ODSN_SHOWCASE_CREATED, "iri", String.class);

        String showcaseUpdateTurtle = ResourceUtils.loadTurtle("/showcase-mietpreisentwicklung.ttl", showcaseId, "Updated Mietpreisentwicklung in Bern");

        String askIfShowcaseExists = """
                %s
                ASK {
                    GRAPH ?g {
                        <%s> a <https://example.org/Showcase> ;
                           dct:identifier "%s" ;
                           dct:title "Updated Mietpreisentwicklung in Bern"@de .
                    }
                }
                """.formatted(PREFIXES, showcaseIRI, showcaseId);

        assertFalse(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfShowcaseExists));

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(showcaseUpdateTurtle).when().put("/customresources/showcase?catalogId=" + catalogId + "&id=" + showcaseId).then().statusCode(is(oneOf(200, 204)));

        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfShowcaseExists));
    }

    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_UPDATED)
    @Provides(Goal.ODSN_SHOWCASE_INDEX_UPDATED)
    public void indexShowcaseAfterUpdate(TestContext context) {
        String showcaseId = context.get(Goal.ODSN_SHOWCASE_CREATED, "id", String.class);

        System.out.println("Checking Showcase Document after update: /resources/showcase/" + showcaseId);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/resources/showcase/" + showcaseId).then().statusCode(200).body("result.id", equalTo(showcaseId)).body("result.title.de", equalTo("Updated Mietpreisentwicklung in Bern"));
        });
    }

    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_INDEX_UPDATED)
    @Provides(Goal.ODSN_SHOWCASE_DELETED)
    public void deleteShowcase(TestContext context) {
        String catalogId = context.get(Goal.ODSN_CATALOG_SHOWCASES_CREATED, "id", String.class);
        String showcaseId = context.get(Goal.ODSN_SHOWCASE_CREATED, "id", String.class);
        String showcaseIRI = context.get(Goal.ODSN_SHOWCASE_CREATED, "iri", String.class);

        String askIfShowcaseExists = """
                %s
                ASK {
                    GRAPH ?g {
                        <%s> a <https://example.org/Showcase> ;
                           dct:identifier "%s" .
                    }
                }
                """.formatted(PREFIXES, showcaseIRI, showcaseId);

        assertTrue(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfShowcaseExists), "showcase exists");

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).when().delete("/customresources/showcase/origin?catalogId=" + catalogId + "&id=" + showcaseId).then().statusCode(is(oneOf(200, 204)));

        // Verify Side Effect: SPARQL
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> !SideEffectUtils.checkSparqlAsk(
                getSparqlEndpoint(), askIfShowcaseExists
        ));
    }

    @Test
    @DependsOn(Goal.ODSN_SHOWCASE_DELETED)
    @Provides(Goal.ODSN_SHOWCASE_INDEX_DELETED)
    public void deleteIndexAfterShowcaseDeletion(TestContext context) {
        String showcaseId = context.get(Goal.ODSN_SHOWCASE_CREATED, "id", String.class);

        System.out.println("Checking Showcase Document after deletion: /resources/showcase/" + showcaseId);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/resources/showcase/" + showcaseId).then().statusCode(404);
        });
    }
    
}
