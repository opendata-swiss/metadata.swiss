package swiss.opendata.piveau.testbench.dag;

import org.junit.jupiter.api.extension.*;
import swiss.opendata.piveau.testbench.Goal;
import swiss.opendata.piveau.testbench.annotations.DependsOn;
import swiss.opendata.piveau.testbench.annotations.Provides;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Extension that:
 * 1. Checks @DependsOn before execution (skips if failed/missing).
 * 2. Updates GoalRegistry after execution (MET or FAILED).
 */
public class DagExtension implements ExecutionCondition, AfterTestExecutionCallback {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<Method> testMethod = context.getTestMethod();
        if (testMethod.isEmpty()) {
            return ConditionEvaluationResult.enabled("Not a method");
        }

        DependsOn dependsOn = testMethod.get().getAnnotation(DependsOn.class);
        if (dependsOn == null) {
            return ConditionEvaluationResult.enabled("No dependencies declared");
        }

        GoalRegistry registry = GoalRegistry.getInstance();
        for (Goal goal : dependsOn.value()) {
            if (registry.isFailed(goal)) {
                return ConditionEvaluationResult.disabled("Dependency FAILED: " + goal);
            }
            if (!registry.isMet(goal)) {
                // In a strict DAG runner, we might want to ensure it runs now.
                // For now, we assume implicit ordering and fail/skip if logic is wrong.
                return ConditionEvaluationResult.disabled("Dependency NOT MET YET (Check Order): " + goal);
            }
        }

        return ConditionEvaluationResult.enabled("All dependencies met");
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        Optional<Method> testMethod = context.getTestMethod();
        if (testMethod.isEmpty()) return;

        Provides provides = testMethod.get().getAnnotation(Provides.class);
        if (provides == null) return;

        GoalRegistry registry = GoalRegistry.getInstance();
        boolean success = context.getExecutionException().isEmpty();

        for (Goal goal : provides.value()) {
            if (success) {
                registry.markMet(goal);
            } else {
                registry.markFailed(goal);
            }
        }
    }
}
