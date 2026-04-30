package swiss.opendata.piveau.testbench.scenarios;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static swiss.opendata.piveau.testbench.TestConstants.API_KEY;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ContainerState;

import swiss.opendata.piveau.testbench.BaseSystemTest;
import swiss.opendata.piveau.testbench.Goal;
import swiss.opendata.piveau.testbench.TestContext;
import swiss.opendata.piveau.testbench.annotations.DependsOn;
import swiss.opendata.piveau.testbench.annotations.Provides;
import swiss.opendata.piveau.testbench.utils.VertxShellUtils;


public class HubInitializationTest extends BaseSystemTest {

    @org.junit.jupiter.api.BeforeEach
    public void setupRestAssured() {
        io.restassured.RestAssured.baseURI = "http://" + getServiceHost("piveau-hub-repo", 8080);
        io.restassured.RestAssured.port = getServicePort("piveau-hub-repo", 8080);
    }

    @Test
    @Provides(Goal.PROFILE_LOADED)
    public void profileLoaded(TestContext context) throws IOException {
        String hubRepoLogs = getHubRepoLogs();
        String hubSearchLogs = getHubSearchLogs();

        assertTrue(hubRepoLogs.contains("Successfully loaded piveau profile 'opendata-swiss' from directory '/piv-profile'"), "Logs should indicate profile loaded");

        assertTrue(hubSearchLogs.contains("Successfully loaded piveau profile 'opendata-swiss' from directory '/piv-profile'"), "Logs should indicate profile loaded");

        assertTrue(hubSearchLogs.contains("Loaded shape successfully for dataset"), "Logs should indicate dataset shape loaded");
        assertTrue(hubSearchLogs.contains("Loaded shape successfully for catalogue"), "Logs should indicate catalogue shape loaded");
        assertTrue(hubSearchLogs.contains("Loaded shape successfully for vocabulary"), "Logs should indicate vocabulary shape loaded");

        assertTrue(hubSearchLogs.contains("Found shape config for resource_showcase"), "Logs should indicate resource_showcase shape config found");
    }

    @Test
    @DependsOn(Goal.PROFILE_LOADED)
    @Provides(Goal.HUB_STARTED)
    public void hubStarted(TestContext context) throws IOException {
        String hubRepoLogs = getHubRepoLogs();
        String hubSearchLogs = getHubSearchLogs();

        assertTrue(hubRepoLogs.contains("Successfully launched server"), "Logs should indicate successful server launch");

        assertTrue(hubSearchLogs.contains("Successfully launched hub-search"), "Logs should indicate successful server launch");
    }

    /**
     * Installs built-in DCAT-AP vocabularies via the hub-repo Vert.x shell.
     * Each vocabulary is installed individually.
     */
    @Test
    @DependsOn({Goal.HUB_STARTED})
    @Provides(Goal.BUILTIN_VOCABS_INSTALLED)
    public void builtinVocabulariesInstalled(TestContext context) throws IOException, InterruptedException {
        ContainerState hubRepo = getContainer("piveau-hub-repo").orElseThrow(() -> new IllegalStateException("piveau-hub-repo container not found"));

        // Select the vocabularies to install by their id from vocabularies.json
        List<String> vocabularyIds = List.of(
                "data-theme", "file-type", "frequency", "language", "licence", "access-right", "dataset-type", "distribution-type", "planned-availability"
        );

        assertVocabulariesDoNotExist(vocabularyIds);

        for (String vocabId : vocabularyIds) {
            Container.ExecResult result = VertxShellUtils.executeShellCommand(
                    hubRepo, "installVocabularies " + vocabId, 120
            );
            String output = result.getStdout() + result.getStderr();
            assertTrue(output.contains("Command finished"), "Built-in vocabulary '" + vocabId + "' should install successfully. Output: " + output);
        }

        assertVocabulariesExist(vocabularyIds);
    }

    /**
     * Installs XML vocabularies (iana-media-types, spdx-checksum-algorithm)
     * via the hub-search Vert.x shell.
     */
    @Test
    @DependsOn({Goal.HUB_STARTED})
    @Provides(Goal.BUILTIN_XML_VOCABS_INSTALLED)
    public void xmlVocabulariesInstalled(TestContext context) throws IOException, InterruptedException {
        ContainerState hubSearch = getContainer("piveau-hub-search").orElseThrow(() -> new IllegalStateException("piveau-hub-search container not found"));

        Container.ExecResult result = VertxShellUtils.executeShellCommand(
                hubSearch, "indexXmlVocabularies", "Successfully indexed xml vocabularies", 120
        );
        String output = result.getStdout() + result.getStderr();
        assertTrue(output.contains("Successfully indexed xml vocabularies"), "XML vocabularies should be indexed successfully. Output: " + output);
    }

    /**
     * Installs opendata.swiss custom vocabularies (ch-licenses, showcase-types, legal-forms) via the REST API.
     */
    @Test
    @DependsOn(Goal.HUB_STARTED)
    @Provides(Goal.CUSTOM_ODSN_VOCABS_INSTALLED)
    public void customVocabulariesInstalled(TestContext context) throws IOException {

        List<String> customVocabs = List.of("ch-licenses", "showcase-types", "legal-forms");
        assertVocabulariesDoNotExist(customVocabs);

        File licensesFile = new File("../piveau_vocabularies/licenses-20240716.ttl");
        assertTrue(licensesFile.exists(), "licenses file should exist at " + licensesFile.getAbsolutePath());

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(licensesFile).when().put("/vocabularies/ch-licenses").then().statusCode(is(oneOf(200, 201, 204)));

        File showcaseTypesFile = new File("../piveau_vocabularies/showcase-types.ttl");
        assertTrue(showcaseTypesFile.exists(), "showcase types file should exist at " + showcaseTypesFile.getAbsolutePath());

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(showcaseTypesFile).when().put("/vocabularies/showcase-types").then().statusCode(is(oneOf(200, 201, 204)));

        File legalFormsFile = new File("../piveau_vocabularies/i14y-legalForm.nt");
        assertTrue(legalFormsFile.exists(), "legal forms file should exist at " + legalFormsFile.getAbsolutePath());

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("application/n-triples").body(legalFormsFile).when().put("/vocabularies/legal-forms").then().statusCode(is(oneOf(200, 201, 204)));

        assertVocabulariesExist(customVocabs);
    }


    @Test
    @DependsOn({Goal.HUB_STARTED, Goal.BUILTIN_VOCABS_INSTALLED, Goal.BUILTIN_XML_VOCABS_INSTALLED, Goal.CUSTOM_ODSN_VOCABS_INSTALLED})
    @Provides(Goal.HUB_READY)
    public void hubReady(TestContext context) throws IOException {
        // any additional things to check?
    }

    private void assertVocabulariesDoNotExist(List<String> vocabIds) {
        java.util.List<String> existingVocabs = io.restassured.RestAssured.given().queryParam("valueType", "identifiers").when().get("/vocabularies").then().statusCode(200).extract().jsonPath().getList("$", String.class);

        for (String vocabId : vocabIds) {
            org.junit.jupiter.api.Assertions.assertFalse(existingVocabs.contains(vocabId), "Vocabulary should not exist before installation: " + vocabId);
        }
    }

    private void assertVocabulariesExist(List<String> vocabIds) {
        for (String vocabId : vocabIds) {
            io.restassured.RestAssured.given().when().get("/vocabularies/" + vocabId).then().statusCode(200);
        }
    }

}
