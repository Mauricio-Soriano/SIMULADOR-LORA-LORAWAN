package mx.mauricio.lorawan.network;

import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.gateway.Gateway;

public class NetworkServer {

    public void handleUplink(Device device, Gateway gateway, String payload) {
        System.out.println("[NetworkServer] Uplink from device "
                + device.getDeviceId()
                + " via gateway "
                + gateway.getGatewayId()
                + ": "
                + payload);
    }

    public void receiveFromGateway(String gatewayId, String payload) {
        System.out.println("[NetworkServer] Uplink via gateway "
                + gatewayId
                + ": "
                + payload);
    }
}
