package mx.mauricio.lorawan.frame;

import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.LoRaWAN.FrameHeader;
import mx.mauricio.lorawan.LoRaWAN.MacHeader;

public class UplinkFrame {
    private Device device;
    private ApplicationPayload appPayload;
    private int frameCounter;

    private MacHeader macHeader;
    private FrameHeader frameHeader;
    private int fPort;
    private String mic;

    public UplinkFrame(Device device, ApplicationPayload appPayload) {
        this.device = device;
        this.appPayload = appPayload;
        this.frameCounter = device.getFrameCounter();

        this.macHeader = new MacHeader("40");
        this.frameHeader = new FrameHeader(device.getDeviceId(), frameCounter);
        this.fPort = appPayload.getFPort();
        this.mic = "0000";
    }

    public String toHexString() {
        return String.format("MHDR=40|DEV=%s|FCNT=%04X|FPORT=%d|DATA=%s|MIC=%s",
                device.getDeviceId(),
                frameCounter,
                fPort,
                appPayload.getData(),
                mic);
    }

    @Override
    public String toString() {
        return String.format("[UplinkFrame] %s %s FPort=%d FRMPayload='%s' MIC=%s",
                macHeader,
                frameHeader,
                fPort,
                appPayload.getData(),
                mic);
    }
}