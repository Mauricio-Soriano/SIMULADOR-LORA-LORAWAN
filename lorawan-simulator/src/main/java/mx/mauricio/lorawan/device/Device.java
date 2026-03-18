
package mx.mauricio.lorawan.device;

import mx.mauricio.lorawan.frame.ApplicationPayload;
import mx.mauricio.lorawan.frame.UplinkFrame;
import mx.mauricio.lorawan.gateway.Gateway;

public class Device {

    private final String deviceId;
    private final Gateway gateway;

    // Agregar atributo
    private int frameCounter = 0;

    // Parámetros LoRaWAN
    private final int spreadingFactor;  // SF7-SF12
    private final int bandwidthKHz;     // 125, 250, 500
    private final String codingRate;    // "4/5", "4/6", etc.
    private final int payloadSizeBytes; // 0-255 bytes

    public Device(String deviceId, Gateway gateway) {
        this(deviceId, gateway, 7, 125, "4/5", 20); // Valores por defecto
    }

    // Constructor completo
    public Device(String deviceId, Gateway gateway, 
                  int spreadingFactor, int bandwidthKHz, 
                  String codingRate, int payloadSizeBytes) {
        this.deviceId = deviceId;
        this.gateway = gateway;
        this.spreadingFactor = spreadingFactor;
        this.bandwidthKHz = bandwidthKHz;
        this.codingRate = codingRate;
        this.payloadSizeBytes = payloadSizeBytes;
    }

        // Nuevo método sendUplink
    public void sendUplink(ApplicationPayload appPayload) {
        frameCounter++;
        UplinkFrame frame = new UplinkFrame(this, appPayload);
        System.out.println("[Device " + deviceId + "] " + frame.toHexString());
        gateway.receiveFromDevice(this, frame.toHexString());
    }

    // Getter
    public int getFrameCounter() { return frameCounter; }

    // Getters
    public String getDeviceId() { return deviceId; }
    public int getSpreadingFactor() { return spreadingFactor; }
    public int getBandwidthKHz() { return bandwidthKHz; }
    public String getCodingRate() { return codingRate; }
    public int getPayloadSizeBytes() { return payloadSizeBytes; }

    public void sendUplink(String payload) {
        System.out.println("[Device " + deviceId + "] SF=" + spreadingFactor 
                + " BW=" + bandwidthKHz + "kHz CR=" + codingRate 
                + " Sending " + payloadSizeBytes + "B: " + payload);
        gateway.receiveFromDevice(this, payload);
    }
}
