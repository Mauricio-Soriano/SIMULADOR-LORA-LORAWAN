package mx.mauricio.lorawan.simulator;

import java.util.List;
import mx.mauricio.lorawan.config.LoRaConfig;

public class DeviceProfile {
    private final String deviceId;
    private final LoRaConfig config;
    private final List<String> fields;
    private final int port;

    public DeviceProfile(String deviceId, LoRaConfig config, List<String> fields, int port) {
        this.deviceId = deviceId;
        this.config = config;
        this.fields = fields;
        this.port = port;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public LoRaConfig getConfig() {
        return config;
    }

    public List<String> getFields() {
        return fields;
    }

    public int getPort() {
        return port;
    }
}
