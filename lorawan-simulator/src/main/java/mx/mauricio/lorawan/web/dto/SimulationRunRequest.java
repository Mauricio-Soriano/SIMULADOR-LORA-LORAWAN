package mx.mauricio.lorawan.web.dto;

import java.util.List;

public class SimulationRunRequest {

    public String fileToken; // opcional por compatibilidad
    public String inputFile;

    public SimulationConfig simulation;
    public GatewayConfig gateway;
    public LayoutConfig layout;
    public List<DeviceConfig> devices;
    public ResultOptions resultOptions;

    public static class SimulationConfig {
        public int rowsToProcess;
        public int sendIntervalMs;
    }

    public static class GatewayConfig {
        public String gatewayId;
        public double x;
        public double y;
        public double maxTxPowerDBm;
        public int udpPort;
        public int tcpPort;
    }

    public static class LayoutConfig {
        public String mode;
        public double baseX;
        public double baseY;
        public double distanceMeters;
    }

    public static class DeviceConfig {
        public String deviceId;
        public boolean enabled;
        public String deviceClass;
        public String transport;
        public String config;
        public int fPort;
        public List<Integer> columnIndexes;
        public Position position;
    }

    public static class Position {
        public double x;
        public double y;
    }

    public static class ResultOptions {
        public boolean includeLogs;
        public boolean includePerDeviceStats;
        public boolean includeTimeline;
    }
}