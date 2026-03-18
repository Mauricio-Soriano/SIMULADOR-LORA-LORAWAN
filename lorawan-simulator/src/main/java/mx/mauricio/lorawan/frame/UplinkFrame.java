package mx.mauricio.lorawan.frame;

import mx.mauricio.lorawan.device.Device;

/**
 * Trama Uplink LoRaWAN (PHYPayload tipo uplink)
 */
public class UplinkFrame extends Frame {
    private final ApplicationPayload appPayload;
    private final int spreadingFactor;
    

    public UplinkFrame(Device device, ApplicationPayload appPayload) {
        this.spreadingFactor = device.getSpreadingFactor();
        this.appPayload = appPayload;
        this.frameCounter = device.getFrameCounter(); // Agregar a Device después
    }

    @Override
    public String toHexString() {
        return String.format("Uplink[SF%d|Dev=%02X%02X%02X%02X|FCnt=%d|%s]",
            spreadingFactor,
            devAddress[0], devAddress[1], devAddress[2], devAddress[3],
            frameCounter, appPayload);
    }
}
