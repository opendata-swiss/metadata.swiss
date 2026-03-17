package swiss.opendata.piveau.testbench.utils;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public class ResourceUtils {

    /**
     * Loads a Turtle file from resources, formats it with the provided arguments,
     * and validates that the result is valid Turtle syntax.
     *
     * @param resourcePath Path to the resource file (e.g., "/catalog.ttl")
     * @param args Arguments for string formatting (optional)
     * @return The loaded and formatted Turtle string
     * @throws IOException If the file cannot be read or parsing fails
     */
    public static String loadTurtle(String resourcePath, Object... args) throws IOException {
        try (InputStream is = ResourceUtils.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Resource " + resourcePath + " not found");
            }
            String template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String formatted = template.formatted(args);

            // Verify valid Turtle
            try {
                Rio.parse(new StringReader(formatted), "", RDFFormat.TURTLE);
            } catch (Exception e) {
                throw new IOException("Invalid Turtle syntax in " + resourcePath + ": " + e.getMessage(), e);
            }

            return formatted;
        }
    }
}
