package swiss.opendata.piveau.testbench.scenarios.odsn;

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
import static org.junit.jupiter.api.Assertions.assertFalse;

public class OrganizationHierarchyTest extends BaseSystemTest {

    private static final String ORG_BASE_IRI = "https://opendata.swiss/id/organization/";

    private static final String ID_LEVEL_0 = "kanton-zuerich";
    private static final String ID_LEVEL_1 = "zh-foo";
    private static final String ID_LEVEL_2 = "staatskanzlei-kanton-zuerich";

    @org.junit.jupiter.api.BeforeEach
    public void setupRestAssured() {
        io.restassured.RestAssured.baseURI = "http://" + getServiceHost("piveau-hub-repo", 8080);
        io.restassured.RestAssured.port = getServicePort("piveau-hub-repo", 8080);
    }

    @Test
    @Disabled
    @DependsOn(Goal.HUB_READY)
    @Provides(Goal.ODSN_ORGANIZATION_HIERARCHY_CREATED)
    public void createOrganizationHierarchy(TestContext context) throws IOException {
        // level-0: top-level org, no parent — TTL has no %s placeholder, so no args needed
        String level0Turtle = ResourceUtils.loadTurtle("/org-odsn-zh-kanton-zuerich.ttl");

        // level-1: child of level-0 — TTL has one %s placeholder for the parent ID
        String level1Turtle = ResourceUtils.loadTurtle("/org-odsn-zh-foo.ttl", ID_LEVEL_0);

        // level-2: child of level-1 — TTL has one %s placeholder for the parent ID
        String level2Turtle = ResourceUtils.loadTurtle("/org-odsn-zh-staatskanzlei-kanton-zuerich.ttl", ID_LEVEL_1);

        String askHierarchyExists = """
                %s
                ASK {
                    GRAPH ?g1 { ?level1 org:subOrganizationOf <%s> . }
                    GRAPH ?g2 { ?level2 org:subOrganizationOf ?level1 . }
                }
                """.formatted(PREFIXES, ORG_BASE_IRI + ID_LEVEL_0);

        assertFalse(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askHierarchyExists));

        // Create all three organizations
        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(level0Turtle).when().put("/organizations/" + ID_LEVEL_0).then().statusCode(is(oneOf(200, 201, 204)));

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(level1Turtle).when().put("/organizations/" + ID_LEVEL_1).then().statusCode(is(oneOf(200, 201, 204)));

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(level2Turtle).when().put("/organizations/" + ID_LEVEL_2).then().statusCode(is(oneOf(200, 201, 204)));

        // Verify Side Effect: SPARQL
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askHierarchyExists));

        // TODO: fetch resource IRIs after PUT. store in context

        context.store(Goal.ODSN_ORGANIZATION_HIERARCHY_CREATED, "idLevel0", ID_LEVEL_0);
        context.store(Goal.ODSN_ORGANIZATION_HIERARCHY_CREATED, "idLevel1", ID_LEVEL_1);
        context.store(Goal.ODSN_ORGANIZATION_HIERARCHY_CREATED, "idLevel2", ID_LEVEL_2);
    }
}
