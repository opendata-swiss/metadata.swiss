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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatasetTest extends BaseSystemTest {

    @org.junit.jupiter.api.BeforeEach
    public void setupRestAssured() {
        io.restassured.RestAssured.baseURI = "http://" + getServiceHost("piveau-hub-repo", 8080);
        io.restassured.RestAssured.port = getServicePort("piveau-hub-repo", 8080);
    }

    @Test
    @DependsOn(Goal.ODSN_CATALOG_CREATED)
    @Provides(Goal.ODSN_DATASET_CREATED)
    public void createDataset(TestContext context) throws IOException {
        final String catalogId = context.get(Goal.ODSN_CATALOG_CREATED, "id", String.class);

        final String datasetId = "dataset-" + System.currentTimeMillis();

        String datasetTurtle = ResourceUtils.loadTurtle("/dataset-odsn-abc-forests.ttl", datasetId, "Forests of ABC");

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

        context.store(Goal.ODSN_DATASET_CREATED, "id", datasetId);
        context.store(Goal.ODSN_DATASET_CREATED, "iri", datasetIRI);
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_CREATED)
    @Provides(Goal.ODSN_DATASET_INDEXED)
    public void indexDatasetAfterCreation(TestContext context) {
        String datasetId = context.get(Goal.ODSN_DATASET_CREATED, "id", String.class);

        System.out.println("Checking Dataset Document after creation: /datasets/" + datasetId);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/datasets/" + datasetId).then().statusCode(200).body("result.id", equalTo(datasetId)).body("result.contact_point", hasSize(2)).body("result.contact_point.name", hasItems("Abteilung Wald ABC", "Sekretariat ABC")).body("result.contact_point.email", hasItems("mailto:forests@abc.example.org", "mailto:sekretariat@abc.example.org")).body("result.description.de", equalTo("In diesem Dataset finden Sie Daten zum Waldbestand im Kanton ABC")).body("result.description.en", equalTo("This dataset contains information regarding the forests in Canton ABC")).body("result.description.it", equalTo("Questo set di dati contiene informazioni sulle foreste del Canton ABC")).body("result.identifier", hasItem(datasetId)).body("result.publisher.type", equalTo("Organization")).body("result.publisher.name", equalTo("Verein ABC")).body("result.title.de", equalTo("Waldbestand ABC")).body("result.title.en", equalTo("Forests of ABC")).body("result.title.it", equalTo("Foreste di ABC"));
        });

        // the response is logged, so it's available for example in target/surefire-reports/TEST-swiss.opendata.piveau.testbench.GlobalTestRunner.xml - then search for "indexDatasetAfterCreation" in the logfile
        String json = io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/datasets/" + datasetId).then().log().body().statusCode(200).extract().body().asString();
        context.store(Goal.ODSN_DATASET_INDEXED, "json", json);
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_keyword(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.keywords"), hasSize(4));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.keywords.id"), hasItems("forests", "foreste", "waldbestand"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.keywords.language"), hasItems("en", "it", "de"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_landingPage(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.landing_page"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.landing_page[0].resource"), equalTo("https://abc.example.org/forests/1234/about"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_issued(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.issued"), equalTo("2021-04-26T00:00:00Z"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_spatial(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.spatial_resource"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.spatial_resource[0].resource"), equalTo("http://publications.europa.eu/resource/authority/country/CHE"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_temporal(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.temporal"), hasSize(1));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_theme(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.categories"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.categories[0].resource"), equalTo("http://publications.europa.eu/resource/authority/data-theme/ENVI"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_modified(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.modified"), equalTo("2021-04-26T00:00:00Z"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_accessRights(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.access_right.resource"), equalTo("http://publications.europa.eu/resource/authority/access-right/PUBLIC"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_conformsTo(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.conforms_to"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.conforms_to[0].resource"), equalTo("http://resource.geosciml.org/ontology/timescale/gts"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_page(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.page"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.page[0].resource"), equalTo("https://abc.example.org/forests/1234/about"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_accrualPeriodicity(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.accrual_periodicity.resource"), equalTo("http://publications.europa.eu/resource/authority/frequency/ANNUAL"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_image(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.image"), hasSize(2));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.image"), hasItems("https://opendata.swiss/images/logo_horizontal.png", "https://opendata.swiss/images/logo_default.png"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_isReferencedBy(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.is_referenced_by"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.is_referenced_by"), hasItem("https://data.example.org/data/345"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_language(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.language"), hasSize(2));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.language.resource"), hasItems("http://publications.europa.eu/resource/authority/language/DEU", "http://publications.europa.eu/resource/authority/language/FRA"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_qualifiedAttribution(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.qualified_attribution"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.qualified_attribution[0].had_role"), hasItem("http://inspire.ec.europa.eu/metadata-codelist/ResponsiblePartyRole/pointOfContact"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.qualified_attribution[0].agent.name"), equalTo("Abteilung Wald ABC"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.qualified_attribution[0].agent.homepage"), hasItem("http://www.abc.example.org/forests/"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.qualified_attribution[0].agent.mbox"), hasItem("mailto:forests@abc.example.org"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_qualifiedRelation(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.qualified_relation"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.qualified_relation[0].relation"), hasItem("http://data.example.org/Original987"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.qualified_relation[0].had_role"), hasItem("http://www.iana.org/assignments/relation/original"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_relation(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.relation"), hasSize(3));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.relation"), hasItems("http://www.bafu.admin.ch/laerm/index", "http://www.bafu.admin.ch/legal_info", "http://www.bafu.admin.ch/laerm/about"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_accessUrl(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].access_url"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].access_url[0]"), equalTo("https://data.example.org/data/1234/access"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_license(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].license.resource"), equalTo("http://dcat-ap.ch/vocabulary/licenses/cc-by/4.0"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_description(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].description.en"), equalTo("some description of the data"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].description.de"), equalTo("eine Beschreibung der Daten"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].description.it"), equalTo("una descrizione dei dati"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_format(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].format.resource"), equalTo("http://publications.europa.eu/resource/authority/file-type/CSV"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_rights(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].rights.resource"), equalTo("https://data.example.org/rights/123"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_title(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].title.en"), equalTo("some title of the distribution"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].title.de"), equalTo("ein Titel der Distribution"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].title.it"), equalTo("un titolo di distribuzione"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_modified(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].modified"), equalTo("2019-04-26T00:00:00Z"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_issued(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].issued"), equalTo("2019-04-26T00:00:00Z"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_temporalResolution(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].temporal_resolution"), equalTo("P1D"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_byteSize(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].byte_size"), equalTo(646458));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_checksum(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].checksum.checksum_value"), equalTo("5bcc814127be171c75595d419f371c74c9cf041419c45d6e8d2c789e5c303b47"));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].checksum.algorithm"), equalTo("sha256"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_coverage(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].coverage"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].coverage[0]"), equalTo("2019-09-22"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_page(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].page"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].page[0].resource"), equalTo("https://example.org/1234/about"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_downloadUrl(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].download_url"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].download_url[0]"), equalTo("https://tierstatistik.identitas.ch/data/fig-equids-size.csv"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_identifier(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].identifier"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].identifier[0]"), equalTo("b3577777-10d3-4644-a945-48e1d272ba40"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_image(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].image"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].image[0]"), equalTo("https://opendata.swiss/images/logo_horizontal.png"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_language(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].language"), hasSize(2));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].language.resource"), hasItems("http://publications.europa.eu/resource/authority/language/DEU", "http://publications.europa.eu/resource/authority/language/FRA"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_conformsTo(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].conforms_to"), hasSize(1));
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].conforms_to[0].resource"), equalTo("http://resource.geosciml.org/ontology/timescale/gts"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_mediaType(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].media_type"), equalTo("text/csv"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEXED)
    public void indexDataset_distribution_packageFormat(TestContext context) {
        String json = context.get(Goal.ODSN_DATASET_INDEXED, "json", String.class);
        io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(json);
        org.hamcrest.MatcherAssert.assertThat(jp.get("result.distributions[0].package_format.resource"), equalTo("https://www.iana.org/assignments/media-types/application/zip"));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_FACETED_SEARCH_VERIFIED)
    @Provides(Goal.ODSN_DATASET_UPDATED)
    public void updateDataset(TestContext context) throws IOException {
        String catalogId = context.get(Goal.ODSN_CATALOG_CREATED, "id", String.class);
        String datasetId = context.get(Goal.ODSN_DATASET_CREATED, "id", String.class);
        String datasetIRI = context.get(Goal.ODSN_DATASET_CREATED, "iri", String.class);

        String datasetUpdateTurtle = ResourceUtils.loadTurtle("/dataset-odsn-abc-forests.ttl", datasetId, "Updated Forests of ABC");

        String askIfDatasetExists = """
                %s
                ASK {
                    GRAPH ?g {
                        <%s> a dcat:Dataset ;
                           dct:identifier "%s" ;
                           dct:title "Updated Forests of ABC"@en .
                    }
                }
                """.formatted(PREFIXES, datasetIRI, datasetId);

        assertFalse(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfDatasetExists));

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).contentType("text/turtle").body(datasetUpdateTurtle).when().put("/catalogues/" + catalogId + "/datasets/origin?originalId=" + datasetId).then().statusCode(is(oneOf(200, 204)));

        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfDatasetExists));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_UPDATED)
    @Provides(Goal.ODSN_DATASET_INDEX_UPDATED)
    public void indexDatasetAfterUpdate(TestContext context) {
        String datasetId = context.get(Goal.ODSN_DATASET_CREATED, "id", String.class);

        System.out.println("Checking Dataset Document after update: /datasets/" + datasetId);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/datasets/" + datasetId).then().statusCode(200).body("result.id", equalTo(datasetId)).body("result.title", hasEntry(is(oneOf("en", "de", "fr", "it", "rm")), equalTo("Updated Forests of ABC")));
        });
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_INDEX_UPDATED)
    @Provides(Goal.ODSN_DATASET_DELETED)
    public void deleteDataset(TestContext context) {
        String catalogId = context.get(Goal.ODSN_CATALOG_CREATED, "id", String.class);
        String datasetId = context.get(Goal.ODSN_DATASET_CREATED, "id", String.class);
        String datasetIRI = context.get(Goal.ODSN_DATASET_CREATED, "iri", String.class);

        String askIfDatasetExists = """
                %s
                ASK {
                    GRAPH ?g {
                        <%s> a dcat:Dataset ;
                           dct:identifier "%s" .
                    }
                }
                """.formatted(PREFIXES, datasetIRI, datasetId);

        assertTrue(SideEffectUtils.checkSparqlAsk(getSparqlEndpoint(), askIfDatasetExists), "dataset exists");

        io.restassured.RestAssured.given().header("X-API-Key", API_KEY).when().delete("/catalogues/" + catalogId + "/datasets/origin?originalId=" + datasetId).then().statusCode(is(oneOf(200, 204)));

        // Verify Side Effect: SPARQL
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> !SideEffectUtils.checkSparqlAsk(
                getSparqlEndpoint(), askIfDatasetExists
        ));
    }

    @Test
    @DependsOn(Goal.ODSN_DATASET_DELETED)
    @Provides(Goal.ODSN_DATASET_INDEX_DELETED)
    public void deleteIndexAfterDatasetDeletion(TestContext context) {
        String datasetId = context.get(Goal.ODSN_DATASET_CREATED, "id", String.class);

        System.out.println("Checking Dataset Document after deletion: /datasets/" + datasetId);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            io.restassured.RestAssured.given().baseUri("http://" + getServiceHost(SEARCH_SERVICE_NAME, 8080)).port(getServicePort(SEARCH_SERVICE_NAME, 8080)).when().get("/datasets/" + datasetId).then().statusCode(404);
        });
    }
}
