
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
        System.out.println("[NetworkServer] Uplink recibido vía gateway "
                + gatewayId
                + ": "
                + payload);

        Map<String, String> fields = parsePayload(payload);

        System.out.println("[NetworkServer] Campos parseados:");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }

        if (isValidPayload(fields)) {
            System.out.println("[NetworkServer] Verificación de integridad: OK");

            String deviceId = fields.get("DEV");
            Device device = getRegisteredDevice(deviceId);

            if (device != null) {
                System.out.println("[NetworkServer] Dispositivo identificado: " + deviceId);
                System.out.println("[NetworkServer] Clase del dispositivo: "
                        + device.getConfig().getDeviceClass());
            } else {
                System.out.println("[NetworkServer] Dispositivo no registrado: " + deviceId);
            }

        } else {
            System.out.println("[NetworkServer] Verificación de integridad: ERROR");
        }
    }

    private Map<String, String> parsePayload(String payload) {
        Map<String, String> fields = new HashMap<>();

        String[] parts = payload.split("\\|");
        for (String part : parts) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2) {
                fields.put(keyValue[0], keyValue[1]);
            }
        }

        return fields;
    }

    private boolean isValidPayload(Map<String, String> fields) {
        if (!fields.containsKey("MHDR")) return false;
        if (!fields.containsKey("DEV")) return false;
        if (!fields.containsKey("FCNT")) return false;
        if (!fields.containsKey("FPORT")) return false;
        if (!fields.containsKey("DATA")) return false;
        if (!fields.containsKey("MIC")) return false;

        if (fields.get("MHDR") == null || !fields.get("MHDR").equals("40")) return false;
        if (fields.get("DEV") == null || fields.get("DEV").isEmpty()) return false;
        if (fields.get("FCNT") == null || fields.get("FCNT").isEmpty()) return false;
        if (fields.get("FPORT") == null || fields.get("FPORT").isEmpty()) return false;
        if (fields.get("DATA") == null) return false;
        if (fields.get("MIC") == null || fields.get("MIC").isEmpty()) return false;

        return true;
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
