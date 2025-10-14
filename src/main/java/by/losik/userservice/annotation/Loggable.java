package by.losik.userservice.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    boolean logArgs() default true;
    boolean logResult() default false;
    boolean logExecutionTime() default true;
    Level level() default Level.INFO;

    enum Level {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}