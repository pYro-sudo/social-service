package by.losik.apigateway.aspect;

import by.losik.apigateway.annotation.Loggable;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("@within(by.losik.apigateway.annotation.Loggable)")
    public Object logAnnotatedClass(ProceedingJoinPoint joinPoint) throws Throwable {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        Loggable loggable = AnnotationUtils.findAnnotation(targetClass, Loggable.class);
        return logMethodExecution(joinPoint, loggable);
    }

    @Around("@annotation(by.losik.apigateway.annotation.Loggable)")
    public Object logAnnotatedMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Loggable loggable = AnnotationUtils.getAnnotation((Annotation) joinPoint.getSignature(), Loggable.class);
        return logMethodExecution(joinPoint, loggable);
    }

    private Object logMethodExecution(@NonNull ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
        if (loggable == null) {
            return joinPoint.proceed();
        }

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        boolean logArgs = loggable.logArgs();
        boolean logResult = loggable.logResult();
        Loggable.Level level = loggable.level();

        logAtLevel(level, "[START] {}.{}()", className, methodName);

        if (logArgs && joinPoint.getArgs().length > 0) {
            logAtLevel(level, "[ARGS] {}.{}() args: {}", className, methodName, joinPoint.getArgs());
        }

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            if (result instanceof Mono) {
                return addMonoLogging((Mono<?>) result, className, methodName, startTime, level, logResult);
            }

            if (result instanceof Flux) {
                return addFluxLogging((Flux<?>) result, className, methodName, startTime, level);
            }

            logSyncCompletion(className, methodName, startTime, level, result, logResult);
            return result;

        } catch (Exception e) {
            long time = System.currentTimeMillis() - startTime;
            log.error("[ERROR] {}.{}() failed in {}ms: {}", className, methodName, time, e.getMessage());
            throw e;
        }
    }

    @NonNull
    private <T> Mono<T> addMonoLogging(@NonNull Mono<T> mono, String className, String methodName,
                                       long startTime, Loggable.Level level, boolean logResult) {
        return mono
                .doOnSuccess(result -> {
                    long time = System.currentTimeMillis() - startTime;
                    if (logResult && result != null) {
                        logAtLevel(level, "[END] {}.{}() completed in {}ms with result: {}",
                                className, methodName, time, result);
                    } else {
                        logAtLevel(level, "[END] {}.{}() completed in {}ms", className, methodName, time);
                    }
                })
                .doOnError(error -> {
                    long time = System.currentTimeMillis() - startTime;
                    log.error("[ERROR] {}.{}() failed in {}ms: {}", className, methodName, time, error.getMessage());
                });
    }

    @NonNull
    private <T> Flux<T> addFluxLogging(@NonNull Flux<T> flux, String className, String methodName,
                                       long startTime, Loggable.Level level) {
        return flux
                .doOnComplete(() -> {
                    long time = System.currentTimeMillis() - startTime;
                    logAtLevel(level, "[END] {}.{}() flux completed in {}ms", className, methodName, time);
                })
                .doOnError(error -> {
                    long time = System.currentTimeMillis() - startTime;
                    log.error("[ERROR] {}.{}() flux failed in {}ms: {}", className, methodName, time, error.getMessage());
                });
    }

    private void logSyncCompletion(String className, String methodName, long startTime,
                                   Loggable.Level level, Object result, boolean logResult) {
        long time = System.currentTimeMillis() - startTime;
        if (logResult) {
            logAtLevel(level, "[END] {}.{}() completed in {}ms with result: {}",
                    className, methodName, time, result);
        } else {
            logAtLevel(level, "[END] {}.{}() completed in {}ms", className, methodName, time);
        }
    }

    private void logAtLevel(@NonNull Loggable.Level level, String message, Object... args) {
        switch (level) {
            case DEBUG -> log.debug(message, args);
            case INFO -> log.info(message, args);
            case WARN -> log.warn(message, args);
            case ERROR -> log.error(message, args);
        }
    }
}