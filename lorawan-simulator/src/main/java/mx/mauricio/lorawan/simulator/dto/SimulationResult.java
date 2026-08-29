package mx.mauricio.lorawan.simulator.dto;

import java.util.ArrayList;
import java.util.List;

public class SimulationResult {

    private boolean success;
    private int rowsProcessed;
    private int rowsSkipped;
    private int devicesConfigured;
    private String message;
    private String scenarioName;

    private List<DeviceMetric> metrics =
            new ArrayList<>();

    public SimulationResult() {
    }

    public SimulationResult(
            boolean success,
            int rowsProcessed,
            int rowsSkipped,
            int devicesConfigured,
            String message) {

        this.success = success;
        this.rowsProcessed = rowsProcessed;
        this.rowsSkipped = rowsSkipped;
        this.devicesConfigured = devicesConfigured;
        this.message = message;
    }

    public static class DeviceMetric {

        public String deviceId;

        public int txAttempts;
        public int originalMessages;
        public int retransmissions;

        public int rx;
        public int lost;
        public int lostByLinkBudget;
        public int lostByRandom;

        public double pdr;
        public double deliveryRate;

        public int confirmedMessages;
        public int ackGenerated;
        public int ackReceived;
        public int ackLost;
        public double confirmedSuccessRate;

        public double rssiAvg;
        public double linkMarginAvg;

        public double throughputKbps;
        public double latencyAvgMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getRowsProcessed() {
        return rowsProcessed;
    }

    public void setRowsProcessed(int rowsProcessed) {
        this.rowsProcessed = rowsProcessed;
    }

    public int getRowsSkipped() {
        return rowsSkipped;
    }

    public void setRowsSkipped(int rowsSkipped) {
        this.rowsSkipped = rowsSkipped;
    }

    public int getDevicesConfigured() {
        return devicesConfigured;
    }

    public void setDevicesConfigured(int devicesConfigured) {
        this.devicesConfigured = devicesConfigured;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public List<DeviceMetric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<DeviceMetric> metrics) {
        this.metrics = metrics != null
                ? metrics
                : new ArrayList<>();
    }
}