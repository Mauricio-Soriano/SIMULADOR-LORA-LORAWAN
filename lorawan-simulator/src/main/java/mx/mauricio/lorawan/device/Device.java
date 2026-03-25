package mx.mauricio.lorawan.device;

//import mx.mauricio.lorawan.LoRaWAN.PaqueteUplink;
import mx.mauricio.lorawan.communication.UdpSender;
import mx.mauricio.lorawan.config.LoRaConfig;
import mx.mauricio.lorawan.frame.ApplicationPayload;
import mx.mauricio.lorawan.frame.UplinkFrame;
import mx.mauricio.lorawan.gateway.Gateway;
//import mx.mauricio.lorawan.communication.UdpSender;
//import mx.mauricio.lorawan.communication.TcpSender;
//import mx.mauricio.lorawan.config.DeviceClass;



public class Device {
    private final String deviceId;
    private final Gateway gateway;
    private final LoRaConfig config;
    private int frameCounter = 0;

    // Constructor principal con LoRaConfig
    public Device(String deviceId, Gateway gateway, LoRaConfig config) {
        this.deviceId = deviceId;
        this.gateway = gateway;
        this.config = config;
    }

    // Constructor simple (usa config por defecto)
    public Device(String deviceId, Gateway gateway) {
        this(deviceId, gateway, LoRaConfig.US915_CLASS_A);
    }
/************ SendUPlink Con Sockets actualizado*************/

public void sendUplink(ApplicationPayload appPayload) {
    frameCounter++;
    UplinkFrame frame = new UplinkFrame(this, appPayload);

    System.out.printf("[Device %s %s SF%d] %s%n",
            deviceId,
            config.getDeviceClass(),
            config.getSpreadingFactor(),
            frame);

    UdpSender sender = new UdpSender("127.0.0.1", 5000);
    sender.send(frame.toHexString());
}


/************ SendUPlink Con Sockets*************/
/* 
 public void sendUplink(ApplicationPayload appPayload) {
    frameCounter++;
    UplinkFrame frame = new UplinkFrame(this, appPayload);

    System.out.printf("[Device %s %s SF%d] %s%n",
            deviceId,
            config.getDeviceClass(),
            config.getSpreadingFactor(),
            frame);

    UdpSender sender = new UdpSender("127.0.0.1", 5000);
    sender.send(frame.toHexString());
}
*/

    /*
    public void sendUplink(ApplicationPayload appPayload) {
        frameCounter++;
        UplinkFrame frame = new UplinkFrame(this, appPayload);

        System.out.printf("[Device %s %s SF%d] %s%n",
                deviceId,
                config.getDeviceClass(),
                config.getSpreadingFactor(),
                frame);

        gateway.receiveFromDevice(this, frame.toHexString());
    }

     */
    // Getters
    public String getDeviceId() { return deviceId; }
    public LoRaConfig getConfig() { return config; }
    public int getFrameCounter() { return frameCounter; }
    public int getSpreadingFactor() { return config.getSpreadingFactor(); }
}
