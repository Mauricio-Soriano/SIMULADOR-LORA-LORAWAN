package mx.mauricio.lorawan.network;

import java.util.Map;


import mx.mauricio.lorawan.device.Device;

import mx.mauricio.lorawan.gateway.Gateway;

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


    private final PerformanceMetricsStore performanceMetricsStore;
    private final UplinkProcessor uplinkProcessor =
    new UplinkProcessor();
    private final DownlinkProcessor downlinkProcessor =
        new DownlinkProcessor();
    private final LinkBudgetService linkBudgetService;

    private final AdrManager adrManager =
        new AdrManager();

    private final DuplicateFrameDetector duplicateDetector =
        new DuplicateFrameDetector();

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

        String fcntString =
                fields.get("FCNT");

        int currentFcnt =
                Integer.parseInt(
                        fcntString,
                        16);

        String deviceId =
                context.getDeviceId();

        Device device = getRegisteredDevice(deviceId);


        DeviceSession session =
            sessionRegistry.getOrCreate(deviceId);
        
        
        if (duplicateDetector.isDuplicate(
                session,
                fields.get("FCNT"))) {

            System.out.println(
                    "[NetworkServer] DUPLICATE UPLINK detectado para "
                    + deviceId
                    + " FCNT="
                    + fields.get("FCNT"));

            return;
        }
        
        
        session.incrementPacketsTransmitted();
        session.incrementPacketsReceived();

        if (session.getLastFcnt() >= 0) {

            int expected =
                    session.getLastFcnt() + 1;

            if (currentFcnt > expected) {

                int lost =
                        currentFcnt - expected;

                session.incrementPacketsLost(lost);

                System.out.println(
                        "[Metrics] "
                                + deviceId
                                + " PDR="
                                + String.format(
                                        "%.2f",
                                        session.getPdr())
                                + "%");
            }
        }

        session.setLastFcnt(currentFcnt);

        System.out.println(
        "[Metrics] "
                + deviceId
                + " Rx="
                + session.getPacketsReceived()
                + " Lost="
                + session.getPacketsLost());
        



        session.incrementFCntUp();

        if (context.isConfirmed()) {

            session.setAckRequired(true);

            System.out.println(
                "[DeviceSession] "
                + deviceId
                + " ACK requerido"
            );
        }

        System.out.println(
            "[DEBUG] ackRequired="
            + session.isAckRequired()
        );


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

    int recommendedSf =
            adrManager.recommendSpreadingFactor(
                    session.getLastRssi());

    session.setRecommendedSf(
            recommendedSf);

    System.out.println(
            "[ADR] "
            + device.getDeviceId()
            + " recomendado SF"
            + recommendedSf);

    System.out.println(
        "[DeviceSession] "
        + deviceId
        + " RSSI="
        + result.getRxPowerDbm()
        + " SNR="
        + result.getMarginDb()
    );
}

        if (gateway != null) {

            downlinkProcessor.process(
                    device,
                    session,
                    gateway,
                    context,
                    transport
            );
            
        }
    
    }
    public DeviceSessionRegistry getSessionRegistry() {
        return sessionRegistry;
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