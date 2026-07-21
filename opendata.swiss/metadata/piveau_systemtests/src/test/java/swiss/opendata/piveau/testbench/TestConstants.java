package swiss.opendata.piveau.testbench;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestConstants {

    public static final String SCENARIOS_PACKAGE = "swiss.opendata.piveau.testbench.scenarios";

    public static final String API_KEY = resolveApiKey();
    public static final String SEARCH_SERVICE_NAME = "piveau-hub-search";

    public static final String PREFIXES = """
            PREFIX dcat: <http://www.w3.org/ns/dcat#>
            PREFIX dct: <http://purl.org/dc/terms/>
            PREFIX foaf: <http://xmlns.com/foaf/0.1/>
            PREFIX org: <http://www.w3.org/ns/org#>
            """;

    private static String resolveApiKey() {
        String apiKey = readApiKeyFromDotEnv();
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }

        throw new IllegalStateException("Missing PIVEAU_HUB_API_KEY in .env file.");
    }

    private static String readApiKeyFromDotEnv() {
        Path dotEnvPath = Path.of(".env");
        if (!Files.exists(dotEnvPath)) {
            return null;
        }

        try {
            List<String> lines = Files.readAllLines(dotEnvPath);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).trim();
                }

                int separatorIndex = line.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }

                String key = line.substring(0, separatorIndex).trim();
                if (!"PIVEAU_HUB_API_KEY".equals(key)) {
                    continue;
                }

                String value = line.substring(separatorIndex + 1).trim();
                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }

                return value;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read .env file for PIVEAU_HUB_API_KEY", e);
        }

        return null;
    }
}
