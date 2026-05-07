package mx.mauricio.lorawan.network;

import java.util.HashMap;
import java.util.Map;

import mx.mauricio.lorawan.config.DeviceClass;
import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.frame.DownlinkFrame;
import mx.mauricio.lorawan.gateway.Gateway;
import mx.mauricio.lorawan.network.policy.ClassADownlinkPolicy;
import mx.mauricio.lorawan.network.policy.ClassBDownlinkPolicy;
import mx.mauricio.lorawan.network.policy.ClassCDownlinkPolicy;
import mx.mauricio.lorawan.network.policy.DownlinkDecision;
import mx.mauricio.lorawan.network.policy.DownlinkPolicy;

public class NetworkServer {

    private final Map<String, Device> registeredDevices = new HashMap<>();
    private final Map<String, Gateway> registeredGateways = new HashMap<>();
    private final Map<String, Integer> downlinkCounters = new HashMap<>();

    private final DownlinkPolicy classAPolicy = new ClassADownlinkPolicy();
    private final DownlinkPolicy classBPolicy = new ClassBDownlinkPolicy();
    private final DownlinkPolicy classCPolicy = new ClassCDownlinkPolicy();

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
        logParsedFields(fields);

        if (!isValidPayload(fields)) {
            System.out.println("[NetworkServer] Verificación de integridad: ERROR");
            return;
        }

        System.out.println("[NetworkServer] Verificación de integridad: OK");

        String deviceId = fields.get("DEV");
        Device device = getRegisteredDevice(deviceId);

        if (device != null) {
            System.out.println("[NetworkServer] Dispositivo identificado: " + deviceId);
            System.out.println("[NetworkServer] Clase del dispositivo: "
                    + device.getConfig().getDeviceClass());
        } else {
            System.out.println("[NetworkServer] Dispositivo no registrado: " + deviceId);
            return;
        }

        String decodedData = fields.get("DATA");
        System.out.println("[NetworkServer] Payload decodificado: " + decodedData);
        System.out.println("[NetworkServer] Fuente decodificada: "
                + describeDecodedSource(fields.get("FPORT"), decodedData));

        evaluateDownlinkPolicy(device, fields, gatewayId, transport);
    }

    private void evaluateDownlinkPolicy(Device device,
                                        Map<String, String> fields,
                                        String gatewayId,
                                        String transport) {

        DownlinkPolicy policy = resolvePolicy(device);
        DownlinkDecision decision = policy.evaluate(device, fields, transport);

        if (!decision.shouldSend()) {
            return;
        }

        sendCommandDownlink(
                gatewayId,
                device.getDeviceId(),
                decision.getCommand(),
                decision.getFPort(),
                transport
        );
    }

    private DownlinkPolicy resolvePolicy(Device device) {
        DeviceClass deviceClass = device.getConfig().getDeviceClass();

        switch (deviceClass) {
            case CLASS_A:
                return classAPolicy;
            case CLASS_B:
                return classBPolicy;
            case CLASS_C:
                return classCPolicy;
            default:
                return (d, f, t) -> DownlinkDecision.none();
        }
    }

    private void sendCommandDownlink(String gatewayId,
                                     String devAddr,
                                     String command,
                                     String fPort,
                                     String transport) {

        String fCnt = nextDownlinkCounter(devAddr);
        DownlinkFrame downlink = new DownlinkFrame(devAddr, fCnt, fPort, command);
        String payload = downlink.toPayloadString();

        System.out.println("[NetworkServer] Generando Downlink para " + devAddr + ": " + command);
        System.out.println("[NetworkServer] FCNT downlink asignado: " + fCnt);
        System.out.println("[NetworkServer] Enviando Downlink vía gateway "
                + gatewayId + " (" + transport + ")...");

        Gateway gateway = getRegisteredGateway(gatewayId);

        if (gateway == null) {
            System.out.println("[NetworkServer] ERROR: Gateway no encontrado: " + gatewayId);
            return;
        }

        gateway.sendDownlink(payload, transport);
    }

    private String nextDownlinkCounter(String devAddr) {
        int nextValue = downlinkCounters.getOrDefault(devAddr, 0) + 1;
        downlinkCounters.put(devAddr, nextValue);
        return String.format("%04X", nextValue);
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

    private void logParsedFields(Map<String, String> fields) {
        System.out.println("[NetworkServer] Campos parseados:");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }
    }

    private boolean isValidPayload(Map<String, String> fields) {
        if (!fields.containsKey("MHDR")) return false;
        if (!fields.containsKey("DEV")) return false;
        if (!fields.containsKey("FCNT")) return false;
        if (!fields.containsKey("FPORT")) return false;
        if (!fields.containsKey("DATA")) return false;
        if (!fields.containsKey("MIC")) return false;

        if (!"40".equals(fields.get("MHDR"))) return false;
        if (isBlank(fields.get("DEV"))) return false;
        if (isBlank(fields.get("FCNT"))) return false;
        if (isBlank(fields.get("FPORT"))) return false;
        if (fields.get("DATA") == null) return false;
        if (isBlank(fields.get("MIC"))) return false;

        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isNumeric(String value) {
        if (isBlank(value)) {
            return false;
        }

        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String describeDecodedSource(String fPort, String data) {
        String sourceType;

        switch (fPort) {
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

        return sourceType + " (FPORT " + fPort + ") = " + data;
    }
}