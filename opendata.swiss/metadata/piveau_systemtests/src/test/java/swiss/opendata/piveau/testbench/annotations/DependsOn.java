package swiss.opendata.piveau.testbench.annotations;

import swiss.opendata.piveau.testbench.Goal;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that this test method requires the specified Goal(s) to be met before execution.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DependsOn {
    Goal[] value();
}
