package swiss.opendata.piveau.testbench;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.opentest4j.TestAbortedException;
import swiss.opendata.piveau.testbench.annotations.DependsOn;
import swiss.opendata.piveau.testbench.annotations.Provides;


import java.lang.reflect.Method;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static swiss.opendata.piveau.testbench.TestConstants.SCENARIOS_PACKAGE;

/**
 * The Central Global Test Runner.
 * Scans all test scenarios, builds a Directed Acyclic Graph (DAG) of methods, and executes them in topological order.
 */
public class GlobalTestRunner {

    /**
     * Cleans up Allure results, to prevent test methods from being reported and counted twice.
     */
    @AfterAll
    static void cleanupAllureResults() {
        Path allureResultsDir = Path.of("target/allure-results");
        if (!Files.exists(allureResultsDir)) return;

        try (var files = Files.list(allureResultsDir)) {
            long deleted = files.filter(f -> {
                String name = f.getFileName().toString();
                return name.endsWith("-result.json") || name.endsWith("-container.json");
            }).filter(f -> {
                try {
                    String content = Files.readString(f);
                    return content.contains("swiss.opendata.piveau.testbench.GlobalTestRunner") && !content.contains("swiss.opendata.piveau.testbench.GlobalTestRunnerTest");
                } catch (IOException e) {
                    return false;
                }
            }).peek(f -> {
                try {
                    Files.delete(f);
                } catch (IOException e) {
                    /* ignore */ }
            }).count();
            System.out.println(">>> Allure cleanup: removed " + deleted + " GlobalTestRunner result files.");
        } catch (IOException e) {
            System.err.println(">>> Allure cleanup failed: " + e.getMessage());
        }
    }

    @TestFactory
    public Stream<DynamicTest> executeGlobalDag() {
        // 1. Discover all Tests
        Launcher launcher = LauncherFactory.create();
        LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectPackage(SCENARIOS_PACKAGE)).build();
        TestPlan testPlan = launcher.discover(discoveryRequest);

        Set<TestIdentifier> testIdentifiers = testPlan.getRoots().stream().flatMap(root -> testPlan.getDescendants(root).stream()).filter(id -> id.getType().isTest() && id.getSource().isPresent() && id.getSource().get() instanceof MethodSource).collect(Collectors.toSet());

        // 2. Build DAG Nodes (Method wrappers)
        List<MethodNode> allNodes = new ArrayList<>();
        Map<Goal, MethodNode> goalProviders = new HashMap<>();

        for (TestIdentifier id : testIdentifiers) {
            MethodSource source = (MethodSource) id.getSource().get();
            try {
                Class<?> testClass = Class.forName(source.getClassName());
                // we assume no method overloading and only match on the methodName
                Method method = Arrays.stream(testClass.getDeclaredMethods()).filter(m -> m.getName().equals(source.getMethodName())).findFirst().orElseThrow(() -> new RuntimeException("Method not found: " + source.getMethodName()));

                MethodNode node = new MethodNode(id, method);
                allNodes.add(node);

                Provides provides = method.getAnnotation(Provides.class);
                if (provides != null) {
                    for (Goal goal : provides.value()) {
                        if (goalProviders.containsKey(goal)) {
                            throw new RuntimeException("Goal " + goal + " is provided by multiple methods: " + goalProviders.get(goal).getDisplayName() + " and " + node.getDisplayName());
                        }
                        goalProviders.put(goal, node);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to analyze test method: " + id.getDisplayName(), e);
            }
        }

        // 3. Build Edges
        Map<MethodNode, Set<MethodNode>> dependencies = new HashMap<>();
        for (MethodNode node : allNodes) {
            dependencies.putIfAbsent(node, new HashSet<>());
            DependsOn dependsOn = node.method.getAnnotation(DependsOn.class);
            if (dependsOn != null) {
                for (Goal goal : dependsOn.value()) {
                    MethodNode provider = goalProviders.get(goal);
                    if (provider != null) {
                        dependencies.get(node).add(provider);
                    } else {
                        // Optional: Fail if dependency missing?
                        System.err.println("WARNING: Missing provider for goal " + goal + " required by " + node.method.getName());
                    }
                }
            }
        }

        // 4. Filter (Focused Mode)
        String targetTest = System.getProperty("piveau.target.test");
        List<MethodNode> nodesToRun;
        if (targetTest != null && !targetTest.isBlank()) {
            nodesToRun = filterForTarget(targetTest, allNodes, dependencies);
            System.out.println(">>> FOCUSED EXECUTION: Graph filtered to " + nodesToRun.size() + " methods.");
        } else {
            nodesToRun = allNodes;
        }

        // 5. Topological Sort
        List<MethodNode> sortedNodes = topologicalSort(nodesToRun, node -> dependencies.getOrDefault(node, Collections.emptySet()), Comparator.comparing(MethodNode::getDisplayName), MethodNode::getDisplayName
        );

        // log the sorted nodes
        System.out.println(">>> EXECUTING IN ORDER: " + sortedNodes.stream().map(MethodNode::getDisplayName).collect(Collectors.joining(" <- ")));

        // 6. Generate Dynamic Tests
        java.util.concurrent.atomic.AtomicInteger stepCounter = new java.util.concurrent.atomic.AtomicInteger(1);
        java.util.concurrent.atomic.AtomicInteger passCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger skipCount = new java.util.concurrent.atomic.AtomicInteger(0);
        List<String> failureDetails = Collections.synchronizedList(new ArrayList<>());
        int totalTests = sortedNodes.size();
        TestContext context = new TestContext();

        Stream<DynamicTest> testStream = sortedNodes.stream().map(node -> DynamicTest.dynamicTest(
                node.getDisplayName(), () -> {
                    try {
                        executeSingleWrapper(node, stepCounter.getAndIncrement(), context);
                        passCount.incrementAndGet();
                    } catch (TestAbortedException e) {
                        skipCount.incrementAndGet();
                        throw e;
                    } catch (Throwable e) {
                        failCount.incrementAndGet();
                        failureDetails.add(node.getDisplayName());
                        throw e;
                    }
                }
        ));

        DynamicTest summaryTest = DynamicTest.dynamicTest("=== SUMMARY ===", () -> {
            System.out.println();
            System.out.println("========================================");
            System.out.println("  TESTBENCH SUMMARY");
            System.out.println("========================================");
            System.out.println("  Total:   " + totalTests);
            System.out.println("  Passed:  " + passCount.get());
            System.out.println("  Failed:  " + failCount.get());
            System.out.println("  Skipped: " + skipCount.get());
            if (!failureDetails.isEmpty()) {
                System.out.println("  ----------------------------------------");
                System.out.println("  Failures:");
                for (String detail : failureDetails) {
                    System.out.println("    - " + detail);
                }
            }
            System.out.println("========================================");
            System.out.println();
        });

        return Stream.concat(testStream, Stream.of(summaryTest));
    }

    private void executeSingleWrapper(MethodNode node, int stepNumber, TestContext context) throws Throwable {
        System.out.println(String.format(">>> STEP %d: %s...", stepNumber, node.getDisplayName()));

        Launcher launcher = LauncherFactory.create();
        MethodSource source = (MethodSource) node.id.getSource().get();

        String parameterTypes = Arrays.stream(node.method.getParameterTypes()).map(Class::getName).collect(Collectors.joining(","));
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectMethod(source.getClassName(), source.getMethodName(), parameterTypes)).build();

        // See also TestContextParameterResolver: Used to have TestContext passed into every test method that declares TestContext argument
        // JUnit 5 Launcher API doesn't easily support injecting arguments into methods found via discovery
        // If we would instead instantiate the test class ourselves and invoke the method, then we would lose JUnit features like @BeforeEach, extensions, etc.
        TestContext.setCurrent(context);
        try {
            SummaryGeneratingListener listener = new SummaryGeneratingListener();
            launcher.registerTestExecutionListeners(listener);
            launcher.execute(request);

            TestExecutionSummary summary = listener.getSummary();
            if (summary.getTestsFailedCount() > 0) {
                throw summary.getFailures().get(0).getException();
            }
            if (summary.getTestsSucceededCount() == 0) {
                if (summary.getTestsSkippedCount() > 0) {
                    System.out.println("Test Skipped: " + node.getDisplayName());
                    throw new TestAbortedException("Test Skipped: " + node.getDisplayName());
                }
                throw new RuntimeException("Test did not execute: " + node.getDisplayName());
            }
        } finally {
            TestContext.clear();
        }
    }

    public static <T> List<T> topologicalSort(List<T> nodes, java.util.function.Function<T, Set<T>> dependencyProvider, java.util.Comparator<T> comparator, java.util.function.Function<T, String> displayNameProvider) {
        List<T> result = new ArrayList<>();
        Set<T> visited = new HashSet<>();
        Set<T> seeing = new HashSet<>();

        // Stabilize sort
        List<T> stableNodes = new ArrayList<>(nodes);
        if (comparator != null) {
            stableNodes.sort(comparator);
        }

        for (T node : stableNodes) {
            visitGeneric(node, dependencyProvider, visited, seeing, result, displayNameProvider, comparator);
        }
        return result;
    }

    private static <T> void visitGeneric(T node, java.util.function.Function<T, Set<T>> dependencyProvider, Set<T> visited, Set<T> seeing, List<T> result, java.util.function.Function<T, String> displayNameProvider, java.util.Comparator<T> comparator) {
        if (visited.contains(node)) return;
        if (seeing.contains(node)) throw new RuntimeException("Cycle detected: " + displayNameProvider.apply(node));
        seeing.add(node);

        Set<T> parents = dependencyProvider.apply(node);
        List<T> sortedParents = new ArrayList<>(parents);
        if (comparator != null) {
            sortedParents.sort(comparator);
        }

        for (T parent : sortedParents) {
            visitGeneric(parent, dependencyProvider, visited, seeing, result, displayNameProvider, comparator);
        }

        seeing.remove(node);
        visited.add(node);
        result.add(node);
    }

    private List<MethodNode> filterForTarget(String targetTest, List<MethodNode> allNodes, Map<MethodNode, Set<MethodNode>> dependencies) {
        // targetTest format: ClassName#MethodName
        MethodNode targetNode = allNodes.stream().filter(n -> {
            MethodSource ms = (MethodSource) n.id.getSource().get();
            String ref = ms.getClassName() + "#" + ms.getMethodName();
            return ref.equals(targetTest);
        }).findFirst().orElseThrow(() -> new RuntimeException("Target test not found: " + targetTest));

        Set<MethodNode> required = new HashSet<>();
        collectAncestors(targetNode, dependencies, required);
        return new ArrayList<>(required);
    }

    private void collectAncestors(MethodNode node, Map<MethodNode, Set<MethodNode>> dependencies, Set<MethodNode> collected) {
        if (collected.contains(node)) return;
        collected.add(node);
        for (MethodNode parent : dependencies.getOrDefault(node, Collections.emptySet())) {
            collectAncestors(parent, dependencies, collected);
        }
    }

    private static class MethodNode {
        TestIdentifier id;
        Method method;

        MethodNode(TestIdentifier id, Method method) {
            this.id = id;
            this.method = method;
        }

        String getDisplayName() {
            // return String.format("%s - %s", id.getDisplayName(), method.toString());
            return id.getDisplayName();
        }
    }
}
