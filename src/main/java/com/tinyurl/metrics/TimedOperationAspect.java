package com.tinyurl.metrics;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TimedOperationAspect {

    private final PerformanceMetrics performanceMetrics;

    @Around("@annotation(timedOperation)")
    public Object timedOperation(ProceedingJoinPoint joinPoint, TimedOperation timedOperation) throws Throwable {
        String operationName = timedOperation.value();
        Timer.Sample sample = performanceMetrics.start();
        try {
            return joinPoint.proceed();
        } finally {
            performanceMetrics.stop(sample, operationName);
            log.debug("Timed operation: {} completed", operationName);
        }
    }
}
