package mx.mauricio.lorawan.performance;

public class PerformanceMetric {
    private final long timestampMillis;
    private final LinkBudgetResult linkBudgetResult;

    public PerformanceMetric(long timestampMillis, LinkBudgetResult linkBudgetResult) {
        this.timestampMillis = timestampMillis;
        this.linkBudgetResult = linkBudgetResult;
    }

    public long getTimestampMillis() { return timestampMillis; }
    public LinkBudgetResult getLinkBudgetResult() { return linkBudgetResult; }
}
