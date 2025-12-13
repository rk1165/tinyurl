package com.tinyurl.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
@Slf4j
public class MetricsController {

    private final MeterRegistry meterRegistry;
    private final PerformanceMetrics performanceMetrics;

    @GetMapping("/summary")
    public Map<String, Object> getMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();
        meterRegistry.getMeters().stream()
                .filter(meter -> meter instanceof Timer)
                .filter(meter -> meter.getId().getName().startsWith("tinyurl."))
                .forEach(meter -> {
                    Timer timer = (Timer) meter;
                    String name = timer.getId().getName().replace("tinyurl.", "");

                    Map<String, Object> timerStats = new HashMap<>();
                    timerStats.put("count", timer.count());
                    timerStats.put("meanMs", String.format("%.2f", timer.mean(TimeUnit.MILLISECONDS)));
                    timerStats.put("maxMs", String.format("%.2f", timer.max(TimeUnit.MILLISECONDS)));

                    // Add percentiles
                    var snapshot = timer.takeSnapshot();
                    var percentiles = snapshot.percentileValues();
                    if (percentiles.length > 0) {
                        Map<String, String> percentileMap = new HashMap<>();
                        for (var pv : percentiles) {
                            percentileMap.put("p" + (int)(pv.percentile() * 100), 
                                    String.format("%.2f", pv.value(TimeUnit.MILLISECONDS)));
                        }
                        timerStats.put("percentiles", percentileMap);
                    }
                    summary.put(name, timerStats);
                });
        return summary;
    }

    @GetMapping("/log")
    public String logMetrics() {
        performanceMetrics.logSummary();
        return "Metrics logged. Check application logs.";
    }
}
