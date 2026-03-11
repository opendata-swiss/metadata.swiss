package swiss.opendata.piveau.testbench.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SideEffectUtilsTest {

    private static final String DCAT_DATASET = "http://www.w3.org/ns/dcat#Dataset";
    private static final String DCAT_CATALOG = "http://www.w3.org/ns/dcat#Catalog";

    @Test
    void extractSubjectIri_singleDataset() {
        String turtle = """
                @prefix dcat: <http://www.w3.org/ns/dcat#> .
                @prefix dct:  <http://purl.org/dc/terms/> .

                <https://example.org/dataset/abc123> a dcat:Dataset ;
                    dct:title "My Dataset" ;
                    dct:identifier "abc123" .
                """;

        String iri = SideEffectUtils.extractSubjectIri(turtle, DCAT_DATASET);
        assertEquals("https://example.org/dataset/abc123", iri);
    }

    @Test
    void extractSubjectIri_singleCatalog() {
        String turtle = """
                @prefix dcat: <http://www.w3.org/ns/dcat#> .
                @prefix dct:  <http://purl.org/dc/terms/> .

                <https://example.org/catalog/mycat> a dcat:Catalog ;
                    dct:title "My Catalog" .
                """;

        String iri = SideEffectUtils.extractSubjectIri(turtle, DCAT_CATALOG);
        assertEquals("https://example.org/catalog/mycat", iri);
    }

    @Test
    void extractSubjectIri_noMatch() {
        String turtle = """
                @prefix dcat: <http://www.w3.org/ns/dcat#> .
                @prefix dct:  <http://purl.org/dc/terms/> .

                <https://example.org/dataset/abc123> a dcat:Dataset ;
                    dct:title "My Dataset" .
                """;

        RuntimeException ex = assertThrows(RuntimeException.class, () -> SideEffectUtils.extractSubjectIri(turtle, DCAT_CATALOG));

        assertTrue(ex.getMessage().contains("No subject found"), "Expected 'No subject found' but got: " + ex.getMessage());
    }

    @Test
    void extractSubjectIri_multipleSubjects_throws() {
        String turtle = """
                @prefix dcat: <http://www.w3.org/ns/dcat#> .
                @prefix dct:  <http://purl.org/dc/terms/> .

                <https://example.org/dataset/one> a dcat:Dataset ;
                    dct:title "First" .

                <https://example.org/dataset/two> a dcat:Dataset ;
                    dct:title "Second" .
                """;

        RuntimeException ex = assertThrows(RuntimeException.class, () -> SideEffectUtils.extractSubjectIri(turtle, DCAT_DATASET));

        assertTrue(ex.getMessage().contains("Multiple subjects found"), "Expected 'Multiple subjects found' but got: " + ex.getMessage());
    }

    @Test
    void extractSubjectIri_differentTypes_noConflict() {
        String turtle = """
                @prefix dcat: <http://www.w3.org/ns/dcat#> .
                @prefix dct:  <http://purl.org/dc/terms/> .

                <https://example.org/catalog/mycat> a dcat:Catalog ;
                    dct:title "My Catalog" .

                <https://example.org/dataset/abc123> a dcat:Dataset ;
                    dct:title "My Dataset" .
                """;

        assertEquals("https://example.org/catalog/mycat", SideEffectUtils.extractSubjectIri(turtle, DCAT_CATALOG));

        assertEquals("https://example.org/dataset/abc123", SideEffectUtils.extractSubjectIri(turtle, DCAT_DATASET));
    }

    @Test
    void extractSubjectIri_invalidTurtle_throws() {
        String badTurtle = "this is not valid turtle";

        assertThrows(RuntimeException.class, () -> SideEffectUtils.extractSubjectIri(badTurtle, DCAT_DATASET));
    }
}
