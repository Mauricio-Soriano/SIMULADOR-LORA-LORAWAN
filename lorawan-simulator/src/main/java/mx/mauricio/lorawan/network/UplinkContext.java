package mx.mauricio.lorawan.network;

import java.util.Map;

public class UplinkContext {

    private final Map<String, String> fields;
    private final String deviceId;
    private final String decodedPayload;
    private final String description;

    public UplinkContext(
            Map<String, String> fields,
            String deviceId,
            String decodedPayload,
            String description) {

        this.fields = fields;
        this.deviceId = deviceId;
        this.decodedPayload = decodedPayload;
        this.description = description;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDecodedPayload() {
        return decodedPayload;
    }

    public String getDescription() {
        return description;
    }
}