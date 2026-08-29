package mx.mauricio.lorawan.web.dto;

import java.util.ArrayList;
import java.util.List;
import mx.mauricio.lorawan.simulator.dto.SimulationResult.DeviceMetric;

public class SimulationRunResponse {
    public boolean success;
    public String simulationId;
    public String message;
    public String scenarioName;
    
    public Summary summary = new Summary();
    public List<DeviceResult> devices = new ArrayList<>();
    public List<EventItem> events = new ArrayList<>();
    public List<DeviceMetric> metrics = new ArrayList<>();

    public static class Summary {
        public int rowsProcessed;
        public int rowsSkipped;
        public int devicesConfigured;
        public int uplinksSent;
        public int uplinksFailed;
        public int downlinksReceived;
        public long durationMs;
    }

    public static class DeviceResult {
        public String deviceId;
        public String deviceClass;
        public String transport;
        public List<Integer> columnIndexes;
        public int rowsProcessed;
        public int uplinksSent;
        public int uplinksFailed;
        public int downlinksReceived;
        public String lastPayload;
        public double avgMarginDb;
    }

    public static class EventItem {
        public String timestamp;
        public String level;
        public String deviceId;
        public String message;
    }
}