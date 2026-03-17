package swiss.opendata.piveau.testbench.dag;

import swiss.opendata.piveau.testbench.Goal;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton registry tracking the state of all Goals.
 */
public class GoalRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(GoalRegistry.class);
    private static final GoalRegistry INSTANCE = new GoalRegistry();

    private final Set<Goal> metGoals = ConcurrentHashMap.newKeySet();
    private final Set<Goal> failedGoals = ConcurrentHashMap.newKeySet();

    private GoalRegistry() {
    }

    public static GoalRegistry getInstance() {
        return INSTANCE;
    }

    public void markMet(Goal goal) {
        LOG.info("GOAL MET: {}", goal);
        metGoals.add(goal);
    }

    public void markFailed(Goal goal) {
        LOG.error("GOAL FAILED: {}", goal);
        failedGoals.add(goal);
    }

    public boolean isMet(Goal goal) {
        return metGoals.contains(goal);
    }

    public boolean isFailed(Goal goal) {
        return failedGoals.contains(goal);
    }

    public void reset() {
        metGoals.clear();
        failedGoals.clear();
    }
}
