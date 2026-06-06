package mx.mauricio.lorawan.web.dto;

import java.util.ArrayList;
import java.util.List;

public class SimulationRunRequest {

    public String fileToken;
    public String delimiter;
    public boolean hasHeader;

    public SimulationConfig simulation;
    public GatewayConfig gateway;
    public List<DeviceConfig> devices = new ArrayList<>();

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

    public static class DeviceConfig {
        public String deviceId;
        public String deviceClass;
        public String transport;
        public int fPort;
        public boolean enabled = true;
        public List<Integer> columnIndexes = new ArrayList<>();
    }
}