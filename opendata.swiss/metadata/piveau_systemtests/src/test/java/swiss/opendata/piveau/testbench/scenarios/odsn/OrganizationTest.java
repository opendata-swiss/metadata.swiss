package swiss.opendata.piveau.testbench.scenarios.odsn;

import org.eclipse.rdf4j.model.vocabulary.FOAF;
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

public class OrganizationTest extends BaseSystemTest {

    @org.junit.jupiter.api.BeforeEach
    public void setupRestAssured() {
        io.restassured.RestAssured.baseURI = "http://" + getServiceHost("piveau-hub-repo", 8080);
        io.restassured.RestAssured.port = getServicePort("piveau-hub-repo", 8080);
    }

    @Test
    @DependsOn(Goal.HUB_READY)
    @Provides(Goal.ODSN_ORGANIZATION_CREATED)
    public void createOrganization(TestContext context) throws IOException {
        final long timestamp = System.currentTimeMillis();
        final String organizationId = "test-organization-" + timestamp;
        final String organizationName = "Test Organization " + timestamp;

        String organizationTurtle = ResourceUtils.loadTurtle("/organization-kt-zh.ttl", organizationName);

        String askIfOrganizationExists = """
                %s
                ASK {
                    GRAPH ?g {
                        ?organization
                        a foaf:Organization ;
                           foaf:name "%s"@en .
                    }
                }
                """.formatted(PREFIXES, organizationName);

        assertFalse(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfOrganizationExists));

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(organizationTurtle).when().put("/organizations/" + organizationId).then().statusCode(is(oneOf(200, 201, 204)));

        // Verify Side Effect: SPARQL
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> SideEffectUtils.checkSparqlAsk(
                getSparqlEndpoint(), askIfOrganizationExists
        ));

        // Extract the minted organization IRI from the API
        String organizationRdf = io.restassured.RestAssured.given().accept("text/turtle").when().get("/organizations/" + organizationId).then().statusCode(200).extract().body().asString();
        String organizationIRI = SideEffectUtils.extractSubjectIri(organizationRdf, FOAF.ORGANIZATION.stringValue());
        System.out.println("Minted Organization IRI: " + organizationIRI);

        assertNotNull(organizationIRI);
        assertTrue(organizationIRI.startsWith("https://opendata.swiss/id/organization/"));

        context.store(Goal.ODSN_ORGANIZATION_CREATED, "id", organizationId);
        context.store(Goal.ODSN_ORGANIZATION_CREATED, "iri", organizationIRI);
        context.store(Goal.ODSN_ORGANIZATION_CREATED, "name", organizationName);
    }

    // @Test
    // @DependsOn(Goal.ODSN_ORGANIZATION_CREATED)
    // @Provides(Goal.ODSN_ORGANIZATION_INDEXED)
    // public void indexOrganizationAfterCreation(TestContext context) {
    //     String organizationId = context.get(Goal.ODSN_ORGANIZATION_CREATED, "id", String.class);
    //     String organizationName = context.get(Goal.ODSN_ORGANIZATION_CREATED, "name", String.class);

    //     System.out.println("Checking Organization Document after creation: /organizations/" + organizationId);
    //     org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
    //         io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/organizations/" + organizationId).then().statusCode(200).body("result.id", equalTo(organizationId)).body("result.name", hasEntry(is(oneOf("en", "de", "fr", "it", "rm")), equalTo(organizationName)));
    //     });
    // }

    @Test
    // @DependsOn(Goal.ODSN_ORGANIZATION_INDEXED)
    @DependsOn(Goal.ODSN_ORGANIZATION_CREATED)
    @Provides(Goal.ODSN_ORGANIZATION_UPDATED)
    public void updateOrganization(TestContext context) throws IOException {
        String organizationId = context.get(Goal.ODSN_ORGANIZATION_CREATED, "id", String.class);
        String oldName = context.get(Goal.ODSN_ORGANIZATION_CREATED, "name", String.class);
        String newName = oldName + " Updated";

        String organizationTurtle = ResourceUtils.loadTurtle("/organization-kt-zh.ttl", newName);

        String askIfOrganizationUpdated = """
                %s
                ASK {
                    GRAPH ?g {
                        ?organization
                        a foaf:Organization ;
                           foaf:name "%s"@en .
                    }
                }
                """.formatted(PREFIXES, newName);

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(organizationTurtle).when().put("/organizations/" + organizationId).then().statusCode(is(oneOf(200, 204)));

        // Verify Side Effect: SPARQL
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> SideEffectUtils.checkSparqlAsk(
                getSparqlEndpoint(), askIfOrganizationUpdated
        ));

        context.store(Goal.ODSN_ORGANIZATION_UPDATED, "name", newName);
    }

    // @Test
    // @DependsOn(Goal.ODSN_ORGANIZATION_UPDATED)
    // @Provides(Goal.ODSN_ORGANIZATION_INDEX_UPDATED)
    // public void indexOrganizationAfterUpdate(TestContext context) {
    //     String organizationId = context.get(Goal.ODSN_ORGANIZATION_CREATED, "id", String.class);
    //     String updatedName = context.get(Goal.ODSN_ORGANIZATION_UPDATED, "name", String.class);

    //     System.out.println("Checking Organization Document after update: /organizations/" + organizationId);
    //     org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
    //         io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/organizations/" + organizationId).then().statusCode(200).body("result.id", equalTo(organizationId)).body("result.name", hasEntry(is(oneOf("en", "de", "fr", "it", "rm")), equalTo(updatedName)));
    //     });
    // }

    @Test
    // @DependsOn(Goal.ODSN_ORGANIZATION_INDEX_UPDATED)
    @DependsOn(Goal.ODSN_ORGANIZATION_UPDATED)
    @Provides(Goal.ODSN_ORGANIZATION_DELETED)
    public void deleteOrganization(TestContext context) {
        String organizationId = context.get(Goal.ODSN_ORGANIZATION_CREATED, "id", String.class);
        String name = context.get(Goal.ODSN_ORGANIZATION_UPDATED, "name", String.class);

        String askIfOrganizationExists = """
                %s
                ASK {
                    GRAPH ?g {
                        ?organization
                        a foaf:Organization ;
                           foaf:name "%s"@en .
                    }
                }
                """.formatted(PREFIXES, name);

        assertTrue(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfOrganizationExists));

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).when().delete("/organizations/" + organizationId).then().statusCode(is(oneOf(200, 204)));

        // Verify Side Effect: SPARQL
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> !SideEffectUtils.checkSparqlAsk(
                getSparqlEndpoint(), askIfOrganizationExists
        ));
    }

    // @Test
    // @DependsOn(Goal.ODSN_ORGANIZATION_DELETED)
    // @Provides(Goal.ODSN_ORGANIZATION_INDEX_DELETED)
    // public void deleteIndexAfterOrganizationDeletion(TestContext context) {
    //     String organizationId = context.get(Goal.ODSN_ORGANIZATION_CREATED, "id", String.class);

    //     System.out.println("Checking Organization Document after deletion: /organizations/" + organizationId);
    //     org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
    //         io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/organizations/" + organizationId).then().statusCode(404);
    //     });
    // }
}
