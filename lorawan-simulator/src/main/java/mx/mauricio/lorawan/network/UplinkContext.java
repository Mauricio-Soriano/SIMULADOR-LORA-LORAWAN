package mx.mauricio.lorawan.network;

import java.util.Map;

public class UplinkContext {

    private final Map<String, String> fields;
    private final String deviceId;
    private final String decodedPayload;
    private final String description;
    private final boolean confirmed;

    public UplinkContext(
            Map<String, String> fields,
            String deviceId,
            String decodedPayload,
            String description, 
            boolean confirmed) {

        this.fields = fields;
        this.deviceId = deviceId;
        this.decodedPayload = decodedPayload;
        this.description = description;
        this.confirmed = confirmed;
    }

    public boolean isConfirmed() {
        return confirmed;
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