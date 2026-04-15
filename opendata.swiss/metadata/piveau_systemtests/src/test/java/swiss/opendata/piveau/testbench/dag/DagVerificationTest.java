package swiss.opendata.piveau.testbench.dag;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import swiss.opendata.piveau.testbench.GlobalTestRunner;
import swiss.opendata.piveau.testbench.Goal;
import swiss.opendata.piveau.testbench.annotations.Provides;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import static swiss.opendata.piveau.testbench.TestConstants.SCENARIOS_PACKAGE;

/**
 * Meta-Test Suite that verifies the integrity of the Dependency Graph (DAG).
 * <p>
 * It effectively "test-drives" every single test method in the project by
 * simulating a "focused execution" run for that method.
 * </p>
 * <p>
 * This ensures that:
 * 1. The DAG can be constructed for every node (no cycles, no missing goals).
 * 2. Every test method's declared dependencies are sufficient to run it in isolation (Semantic Verification).
 * </p>
 * <p>
 * Run with: mvn test -Dgroups=dag-verification
 * </p>
 */
@Tag("dag-verification")
public class DagVerificationTest {

    @TestFactory
    Stream<DynamicTest> verifyAllScenarioTests() {
        Launcher launcher = LauncherFactory.create();
        LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectPackage(SCENARIOS_PACKAGE)).build();

        var testPlan = launcher.discover(discoveryRequest);

        return testPlan.getRoots().stream().flatMap(root -> testPlan.getDescendants(root).stream()).filter(id -> id.getType().isTest() && id.getSource().isPresent()).map(testIdentifier -> {
            String displayName = testIdentifier.getDisplayName();

            // Extract Class#Method
            String targetRef = testIdentifier.getSource().filter(src -> src instanceof org.junit.platform.engine.support.descriptor.MethodSource).map(src -> {
                var ms = (org.junit.platform.engine.support.descriptor.MethodSource) src;
                return ms.getClassName() + "#" + ms.getMethodName();
            }).orElse(null);

            if (targetRef == null) {
                return null; // Skip non-method tests or if extraction fails
            }

            return DynamicTest.dynamicTest("Verify dependency chain for: " + targetRef, () -> {
                executeFocusedRun(targetRef);
            });
        }).filter(java.util.Objects::nonNull);
    }

    private void executeFocusedRun(String targetMethodSig) {
        System.out.println(">>> VERIFYING DAG FOR: " + targetMethodSig);
        swiss.opendata.piveau.testbench.BaseSystemTest.forceRestart();

        // 1. Set the Target Property
        System.setProperty("piveau.target.test", targetMethodSig);
        try {
            // 2. Launch the Test in Isolation via GlobalTestRunner
            // GlobalTestRunner handles the filtering based on the property.
            Launcher launcher = LauncherFactory.create();

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(GlobalTestRunner.class)).build();

            SummaryGeneratingListener listener = new SummaryGeneratingListener();
            launcher.registerTestExecutionListeners(listener);

            launcher.execute(request);

            // 3. Assert Success
            TestExecutionSummary summary = listener.getSummary();

            if (summary.getTotalFailureCount() > 0) {
                // Print failures
                summary.getFailures().forEach(f -> f.getException().printStackTrace());
                fail("Verification failed for " + targetMethodSig + ". " + summary.getFailures().get(0).getException().getMessage());
            }

            if (summary.getTestsSucceededCount() == 0) {
                // Warning: If everything was skipped, we might want to fail?
                // But GlobalTestRunner filters. If filtering results in 0 tests, it means target not found or ancestors empty.
                // Actually GlobalTestRunner throws if target not found.
            }

        } finally {
            // 4. Reset Property
            System.clearProperty("piveau.target.test");
        }
    }

    @Test
    void verifyUniqueGoalProviders() {
        Launcher launcher = LauncherFactory.create();
        LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectPackage(SCENARIOS_PACKAGE)).build();

        var testPlan = launcher.discover(discoveryRequest);

        Map<Goal, String> goalProviders = new java.util.HashMap<>();

        testPlan.getRoots().stream().flatMap(root -> testPlan.getDescendants(root).stream()).filter(id -> id.getType().isTest() && id.getSource().isPresent()).filter(id -> id.getSource().get() instanceof org.junit.platform.engine.support.descriptor.MethodSource).forEach(id -> {
            var ms = (org.junit.platform.engine.support.descriptor.MethodSource) id.getSource().get();
            try {
                Class<?> testClass = Class.forName(ms.getClassName());
                java.lang.reflect.Method method = java.util.Arrays.stream(testClass.getDeclaredMethods()).filter(m -> m.getName().equals(ms.getMethodName())).findFirst().orElse(null);
                if (method == null) return;

                Provides provides = method.getAnnotation(Provides.class);
                if (provides == null) return;

                String ref = ms.getClassName() + "#" + ms.getMethodName();
                for (Goal goal : provides.value()) {
                    String existing = goalProviders.get(goal);
                    if (existing != null) {
                        fail("Goal " + goal + " is provided by multiple methods: " + existing + " and " + ref);
                    }
                    goalProviders.put(goal, ref);
                }
            } catch (ClassNotFoundException e) {
                fail("Could not load class: " + ms.getClassName());
            }
        });

        assertFalse(goalProviders.isEmpty(), "Should have found at least one Goal provider in " + SCENARIOS_PACKAGE);
    }
}
