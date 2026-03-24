package swiss.opendata.piveau.testbench;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TestContext {
    private final Map<Goal, Map<String, Object>> state = new ConcurrentHashMap<>();

    // Store data produced by a test, keyed by Goal and sub-key
    public void store(Goal goal, String key, Object value) {
        state.computeIfAbsent(goal, g -> new ConcurrentHashMap<>()).put(key, value);
    }

    // Retrieve data required by a test
    public <T> T get(Goal goal, String key, Class<T> type) {
        Map<String, Object> goalState = state.get(goal);
        if (goalState == null) {
            throw new IllegalStateException("Test context has no data for goal: " + goal);
        }
        Object value = goalState.get(key);
        if (value == null) {
            throw new IllegalStateException("Test context missing key '" + key + "' for goal: " + goal);
        }
        return type.cast(value);
    }

    // --- ThreadLocal Support for ParameterResolver ---
    private static final ThreadLocal<TestContext> CURRENT = new ThreadLocal<>();

    public static void setCurrent(TestContext context) {
        CURRENT.set(context);
    }

    public static TestContext getCurrent() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
