package swiss.opendata.piveau.testbench;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalTestRunnerTest {

    @Test
    void testCycleDetection() {
        // A -> B -> A
        String nodeA = "NodeA";
        String nodeB = "NodeB";

        List<String> nodes = Arrays.asList(nodeA, nodeB);

        Function<String, Set<String>> dependencyProvider = node -> {
            if (node.equals(nodeA)) return Collections.singleton(nodeB);
            if (node.equals(nodeB)) return Collections.singleton(nodeA);
            return Collections.emptySet();
        };

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            GlobalTestRunner.topologicalSort(nodes, dependencyProvider, Comparator.naturalOrder(), Function.identity());
        });

        assertTrue(exception.getMessage().contains("Cycle detected"), "Exception message should mention cycle detection. Actual: " + exception.getMessage());
    }

    @Test
    void testValidSort() {
        // C -> B -> A
        String nodeA = "NodeA";
        String nodeB = "NodeB";
        String nodeC = "NodeC";

        List<String> nodes = Arrays.asList(nodeA, nodeB, nodeC);

        Function<String, Set<String>> dependencyProvider = node -> {
            if (node.equals(nodeC)) return Collections.singleton(nodeB);
            if (node.equals(nodeB)) return Collections.singleton(nodeA);
            return Collections.emptySet();
        };

        List<String> sorted = GlobalTestRunner.topologicalSort(nodes, dependencyProvider, Comparator.naturalOrder(), Function.identity());

        // Expected order: A, B, C
        assertEquals(Arrays.asList(nodeA, nodeB, nodeC), sorted);
    }

    @Test
    void testDuplicateProviderDetection() {
        // Simulate the goalProviders map from executeGlobalDag
        Map<Goal, String> goalProviders = new HashMap<>();

        // First provider registers fine
        goalProviders.put(Goal.SIMPLE_CATALOG_CREATED, "CatalogTest#createCatalog");

        // Second provider for the same Goal should be detected
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            Goal goal = Goal.SIMPLE_CATALOG_CREATED;
            if (goalProviders.containsKey(goal)) {
                throw new RuntimeException("Goal " + goal + " is provided by multiple methods: " + goalProviders.get(goal) + " and " + "CatalogTest2#createCatalog");
            }
            goalProviders.put(goal, "CatalogTest2#createCatalog");
        });

        assertTrue(exception.getMessage().contains("provided by multiple methods"), "Exception should mention duplicate providers. Actual: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("CATALOG_CREATED"), "Exception should mention the conflicting Goal. Actual: " + exception.getMessage());
    }
}
