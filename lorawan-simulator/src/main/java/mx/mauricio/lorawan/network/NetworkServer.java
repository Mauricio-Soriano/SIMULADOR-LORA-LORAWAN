
/*VERSIÓN ACTUALIZADA PARA EL NS ACTIVIDAD 17.7 */
/*CON ESTA VERSION
***registra dispositivos
***consultar si un deviceId existe
***Recuperar el objeto Device a partir del identificador
*/
package mx.mauricio.lorawan.network;

import java.util.HashMap;
import java.util.Map;

import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.gateway.Gateway;

public class NetworkServer {

    private final Map<String, Device> registeredDevices = new HashMap<>();

    public void registerDevice(Device device) {
        registeredDevices.put(device.getDeviceId(), device);
        System.out.println("[NetworkServer] Dispositivo registrado: "
                + device.getDeviceId()
                + " clase="
                + device.getConfig().getDeviceClass());
    }

    public Device getRegisteredDevice(String deviceId) {
        return registeredDevices.get(deviceId);
    }

    public boolean isRegistered(String deviceId) {
        return registeredDevices.containsKey(deviceId);
    }

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


/* Version NS antes de la identificación de clase */
/*package mx.mauricio.lorawan.network;

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
}*/
