package mx.mauricio.lorawan.performance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PerformanceMetricsStore {
    private final List<PerformanceMetric> metrics = new ArrayList<>();

    public void add(PerformanceMetric metric) {
        metrics.add(metric);
    }

    public List<PerformanceMetric> getAll() {
        return Collections.unmodifiableList(metrics);
    }

    public int size() {
        return metrics.size();
    }

    public void clear() {
        metrics.clear();
    }
}
