package swiss.opendata.piveau.testbench.annotations;

import swiss.opendata.piveau.testbench.Goal;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that this test method achieves the specified Goal(s).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Provides {
    Goal[] value();
}
