package mx.mauricio.lorawan.network;

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
import mx.mauricio.lorawan.performance.LinkBudgetResult;
import mx.mauricio.lorawan.performance.LinkBudgetService;
import mx.mauricio.lorawan.performance.PerformanceMetric;
import mx.mauricio.lorawan.performance.PerformanceMetricsStore;

public class NetworkServer {

    private final DeviceRegistry deviceRegistry = new DeviceRegistry();
    private final DeviceSessionRegistry sessionRegistry =
    new DeviceSessionRegistry();
    private final GatewayRegistry gatewayRegistry =
        new GatewayRegistry();


    private final DownlinkPolicy classAPolicy = new ClassADownlinkPolicy();
    private final DownlinkPolicy classBPolicy = new ClassBDownlinkPolicy();
    private final DownlinkPolicy classCPolicy = new ClassCDownlinkPolicy();

    private final PerformanceMetricsStore performanceMetricsStore;
    private final PayloadParser payloadParser = new PayloadParser();
    private final UplinkProcessor uplinkProcessor =
    new UplinkProcessor();
    private final LinkBudgetService linkBudgetService;

    public NetworkServer() {
        this(new PerformanceMetricsStore(), new LinkBudgetService());
    }

    public NetworkServer(PerformanceMetricsStore performanceMetricsStore,
                         LinkBudgetService linkBudgetService) {
        this.performanceMetricsStore = performanceMetricsStore;
        this.linkBudgetService = linkBudgetService;
    }

    public void registerDevice(Device device) {
        deviceRegistry.register(device);
    }

    public void registerGateway(Gateway gateway) {
        gatewayRegistry.register(gateway);
    }

    public Device getRegisteredDevice(String deviceId) {
        return deviceRegistry.get(deviceId);
    }

    public boolean isRegistered(String deviceId) {
        return deviceRegistry.contains(deviceId);
    }

    public Gateway getRegisteredGateway(String gatewayId) {
        return gatewayRegistry.get(gatewayId);
    }


    //Métodos nuevos fase 2.1
    public boolean isGatewayRegistered(String gatewayId) {
        return gatewayRegistry.contains(gatewayId);
    }

    public int getRegisteredGatewayCount() {
        return gatewayRegistry.size();
    }


    public PerformanceMetricsStore getPerformanceMetricsStore() {
        return performanceMetricsStore;
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

       UplinkContext context =
                uplinkProcessor.process(payload);

        if (context == null) {
            System.out.println(
                "[NetworkServer] Payload inválido.");
            return;
        }

        Map<String, String> fields =
                context.getFields();

        String deviceId =
                context.getDeviceId();

        Device device = getRegisteredDevice(deviceId);


        DeviceSession session =
            sessionRegistry.getOrCreate(deviceId);

        session.incrementFCntUp();


        System.out.println(
            "[DeviceSession] "
            + deviceId
            + " FCntUp="
            + session.getFCntUp()
        );

        session.setLastGatewayId(gatewayId);


        if (device != null) {
            System.out.println("[NetworkServer] Dispositivo identificado: " + deviceId);
            System.out.println("[NetworkServer] Clase del dispositivo: "
                    + device.getConfig().getDeviceClass());
        } else {
            System.out.println("[NetworkServer] Dispositivo no registrado: " + deviceId);
            return;
        }

        System.out.println(
            "[NetworkServer] Payload decodificado: "
            + context.getDecodedPayload());

        System.out.println(
            "[NetworkServer] Fuente decodificada: "
            + context.getDescription());

        Gateway gateway =
        getRegisteredGateway(gatewayId);

if (gateway != null) {

    LinkBudgetResult result =
            registerTransmissionMetric(
                    device,
                    gateway,
                    true);

    session.setLastGatewayId(gatewayId);

    session.setLastRssi(
            result.getRxPowerDbm());

    session.setLastSnr(
            result.getMarginDb());

    System.out.println(
        "[DeviceSession] "
        + deviceId
        + " RSSI="
        + result.getRxPowerDbm()
        + " SNR="
        + result.getMarginDb()
    );
}

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

        DeviceSession session =
                sessionRegistry.getOrCreate(devAddr);

        String fCnt =
                String.format("%04X",
                        session.nextFCntDown());

        System.out.println(
                "[DeviceSession] "
                + devAddr
                + " FCntDown="
                + session.getFCntDown()
        );


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

    public LinkBudgetResult registerTransmissionMetric(Device device, Gateway gateway, boolean los) {
        double dx = gateway.getX();
        double dy = gateway.getY();
        double distanceMeters = Math.sqrt(dx * dx + dy * dy);

        LinkBudgetResult result = linkBudgetService.evaluate(
                device.getDeviceId(),
                gateway.getGatewayId(),
                distanceMeters,
                device.getConfig().getFrequencyMHz(),
                gateway.getMaxTxPowerDBm(),
                -130.0,
                los,
                20.0,
                30.0,
                1.5,
                0.0,
                50.0,
                0.0,
                0.0,
                0.0,
                0.0
        );

        performanceMetricsStore.add(new PerformanceMetric(System.currentTimeMillis(), result));

        System.out.printf(
                "[Performance] dev=%s gw=%s dist=%.2fm loss=%.2fdB rx=%.2fdBm margin=%.2fdB los=%s%n",
                result.getDeviceId(),
                result.getGatewayId(),
                result.getDistanceMeters(),
                result.getPathLossDb(),
                result.getRxPowerDbm(),
                result.getMarginDb(),
                result.isLos()
        );return result;
    }

}