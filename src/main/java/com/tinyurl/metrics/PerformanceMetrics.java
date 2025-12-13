package com.tinyurl.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Slf4j
public class PerformanceMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();

    public PerformanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Time an operation and record the metric
     *
     * @param operationName
     * @param operation
     */
    public void time(String operationName, Runnable operation) {
        Timer timer = getOrCreateTimer(operationName);
        timer.record(operation);
    }

    /**
     * Time an operation that returns a value.
     *
     * @param operationName
     * @param operation
     * @param <T>
     * @return
     */
    public <T> T timeAndReturn(String operationName, Supplier<T> operation) {
        Timer timer = getOrCreateTimer(operationName);
        return timer.record(operation);
    }

    /**
     * Time an operation that may throw a checked exception
     *
     * @param operationName
     * @param operation
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> T timeCallable(String operationName, Callable<T> operation) throws Exception {
        Timer timer = getOrCreateTimer(operationName);
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return operation.call();
        } finally {
            sample.stop(timer);
        }
    }

    /**
     * Start a timer sample for manual timing.
     *
     * @return
     */
    public Timer.Sample start() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop a timer sample and record the duration
     *
     * @param sample
     * @param operationName
     */
    public void stop(Timer.Sample sample, String operationName) {
        Timer timer = getOrCreateTimer(operationName);
        sample.stop(timer);
    }

    private Timer getOrCreateTimer(String operationName) {
        return timers.computeIfAbsent(operationName, name ->
                Timer.builder("tinyurl." + name)
                        .description("Timer for " + name)
                        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                        .register(meterRegistry));
    }

    public void logSummary() {
        log.info("=== Performance Metrics Summary ===");
        timers.forEach((meterName, timer) -> {
            var snapshot = timer.takeSnapshot();
            log.info("Operation: {} | Count: {} | Mean: {}ms | Max: {}ms | p50: {}ms | p75: {}ms | p95: {}ms | p99: {}ms",
                    meterName,
                    timer.count(),
                    String.format("%.2f", timer.mean(TimeUnit.MILLISECONDS)),
                    String.format("%.2f", timer.max(TimeUnit.MILLISECONDS)),
                    String.format("%.2f", snapshot.percentileValues()[0].value(TimeUnit.MILLISECONDS)),
                    String.format("%.2f", snapshot.percentileValues()[1].value(TimeUnit.MILLISECONDS)),
                    String.format("%.2f", snapshot.percentileValues()[2].value(TimeUnit.MILLISECONDS)),
                    String.format("%.2f", snapshot.percentileValues()[3].value(TimeUnit.MILLISECONDS)));
        });
    }
}
