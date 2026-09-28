package mx.mauricio.lorawan.web.dto;

import java.util.ArrayList;
import java.util.List;

import mx.mauricio.lorawan.simulator.dto.SimulationResult.DeviceMetric;

public class SimulationRunResponse {

    public boolean success;
    public String message;
    public String scenarioName;

    public Summary summary =
            new Summary();

    public List<DeviceMetric> metrics =
            new ArrayList<>();

    public static class Summary {

        public int rowsProcessed;
        public int rowsSkipped;
        public int devicesConfigured;
        public long durationMs;
    }
}
