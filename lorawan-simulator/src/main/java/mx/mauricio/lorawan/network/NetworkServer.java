package mx.mauricio.lorawan.network;

import java.util.HashMap;
import java.util.Map;

import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.frame.DownlinkFrame;
import mx.mauricio.lorawan.gateway.Gateway;

public class NetworkServer {

    private final Map<String, Device> registeredDevices = new HashMap<>();
    private final Map<String, Gateway> registeredGateways = new HashMap<>();
    private final Map<String, Integer> downlinkCounters = new HashMap<>();

    public void registerDevice(Device device) {
        registeredDevices.put(device.getDeviceId(), device);
        System.out.println("[NetworkServer] Dispositivo registrado: "
                + device.getDeviceId()
                + " clase="
                + device.getConfig().getDeviceClass());
    }

    public void registerGateway(Gateway gateway) {
        registeredGateways.put(gateway.getGatewayId(), gateway);
        System.out.println("[NetworkServer] Gateway registrado: " + gateway.getGatewayId());
    }

    public Device getRegisteredDevice(String deviceId) {
        return registeredDevices.get(deviceId);
    }

    public boolean isRegistered(String deviceId) {
        return registeredDevices.containsKey(deviceId);
    }

    public Gateway getRegisteredGateway(String gatewayId) {
        return registeredGateways.get(gatewayId);
    }

    private String nextDownlinkCounter(String devAddr) {
        int nextValue = downlinkCounters.getOrDefault(devAddr, 0) + 1;
        downlinkCounters.put(devAddr, nextValue);
        return String.format("%04X", nextValue);
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
        receiveFromGateway(gatewayId, payload, "UDP");
    }

    public void receiveFromGateway(String gatewayId, String payload, String transport) {
        System.out.println("[NetworkServer] Uplink recibido vía gateway "
                + gatewayId
                + " (" + transport + "): "
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

            String decodedData = fields.get("DATA");
            System.out.println("[NetworkServer] Payload decodificado: " + decodedData);
            System.out.println("[NetworkServer] Fuente decodificada: "
                    + describeDecodedSource(fields.get("FPORT"), decodedData));

            evaluateDownlinkLogic(fields, gatewayId, transport);

        } else {
            System.out.println("[NetworkServer] Verificación de integridad: ERROR");
        }
    }

    private void evaluateDownlinkLogic(Map<String, String> fields, String gatewayId, String transport) {
        String fPort = fields.get("FPORT");
        String data = fields.get("DATA");
        String devAddr = fields.get("DEV");

        if ("1".equals(fPort) && isNumeric(data)) {
            double value = Double.parseDouble(data);
            if (value > 27.0) {
                String command = "CMD_ALERT_HIGH";
                String downlinkFCnt = nextDownlinkCounter(devAddr);
                sendDownlink(gatewayId, devAddr, downlinkFCnt, "99", command, transport);
            }
        }

        if ("2".equals(fPort) || "3".equals(fPort)) {
            String command = "CMD_TCP_ACK";
            String downlinkFCnt = nextDownlinkCounter(devAddr);
            sendDownlink(gatewayId, devAddr, downlinkFCnt, "99", command, transport);
        }
    }

    private void sendDownlink(String gatewayId, String devAddr, String fCnt, String fPort, String command, String transport) {
        DownlinkFrame downlink = new DownlinkFrame(devAddr, fCnt, fPort, command);
        String payload = downlink.toPayloadString();

        System.out.println("[NetworkServer] Generando Downlink para " + devAddr + ": " + command);
        System.out.println("[NetworkServer] FCNT downlink asignado: " + fCnt);
        System.out.println("[NetworkServer] Enviando Downlink vía gateway " + gatewayId + " (" + transport + ")...");

        Gateway gateway = getRegisteredGateway(gatewayId);

        if (gateway == null) {
            System.out.println("[NetworkServer] ERROR: Gateway no encontrado: " + gatewayId);
            return;
        }

        gateway.sendDownlink(payload, transport);
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

    private boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) return false;
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String describeDecodedSource(String fport, String data) {
        String sourceType;

        switch (fport) {
            case "1":
                sourceType = "medición de sensor";
                break;
            case "2":
                sourceType = "mensaje de estado";
                break;
            case "3":
                sourceType = "mensaje de control/prueba";
                break;
            default:
                sourceType = isNumeric(data) ? "medición numérica" : "mensaje de aplicación";
                break;
        }

        return sourceType + " (FPORT " + fport + ") = " + data;
    }
}