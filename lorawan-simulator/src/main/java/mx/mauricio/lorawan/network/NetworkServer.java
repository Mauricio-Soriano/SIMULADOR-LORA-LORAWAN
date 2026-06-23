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

    private boolean adrEnabled = true;

    private final DuplicateFrameDetector duplicateDetector =
        new DuplicateFrameDetector();
    
    private final PacketLossSimulator packetLossSimulator =
        new PacketLossSimulator(false, 0.0);

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

    public void configureAdr(boolean enabled) {

        this.adrEnabled =
                enabled;

        System.out.println(
                "[ADR] enabled="
                + enabled);
    }

    //Métodos nuevos fase 2.1
    public boolean isGatewayRegistered(String gatewayId) {
        return gatewayRegistry.contains(gatewayId);
    }

    public int getRegisteredGatewayCount() {
        return gatewayRegistry.size();
    }

    public void configureRandomPacketLoss(
            boolean enabled,
            double probability) {

        packetLossSimulator.setEnabled(enabled);
        packetLossSimulator.setLossProbability(probability);

        System.out.println(
                "[PacketLoss] Random loss enabled="
                + enabled
                + " probability="
                + probability);
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
        
        long rxTimestamp =
            System.currentTimeMillis();

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

            
        long txTimestamp = 0;

        String txTimeString =
                fields.get("TXTIME");

        if(txTimeString != null) {
            txTimestamp =
                    Long.parseLong(txTimeString);
        }


        int currentFcnt =
                Integer.parseInt(
                        fcntString,
                        16);

        String deviceId =
                context.getDeviceId();

        Device device = getRegisteredDevice(deviceId);

        if (device == null) {

            System.out.println(
                    "[NetworkServer] Dispositivo no registrado: "
                    + deviceId);

            return;
        }


        DeviceSession session =
            sessionRegistry.getOrCreate(deviceId);

        
        session.setCurrentSf(
            device.getSpreadingFactor());     

        long latencyMs =
                -1;

        if (txTimestamp > 0) {

            latencyMs =
                    rxTimestamp - txTimestamp;
        }

        session.registerTransmissionAttempt(
        currentFcnt);

        Gateway gateway =
                getRegisteredGateway(gatewayId);

        if (gateway == null) {

            System.out.println(
                    "[NetworkServer] Gateway no registrado: "
                    + gatewayId);

            session.incrementPacketsLost(1);

            return;
        }

        LinkBudgetResult result =
                registerTransmissionMetric(
                        device,
                        gateway,
                        true);

        if (isLinkBudgetDrop(device, result)) {

            System.out.println(
                    "[LinkBudget] PACKET LOST -> "
                    + deviceId
                    + " FCNT="
                    + fields.get("FCNT")
                    + " rx="
                    + String.format("%.2f", result.getRxPowerDbm())
                    + " dBm sens="
                    + String.format(
                            "%.2f",
                            getReceiverSensitivity(
                                    device.getSpreadingFactor()))
                    + " dBm");

            session.incrementLinkBudgetLosses(1);

            return;
        }

        if (packetLossSimulator.shouldDrop()) {

            System.out.println(
                    "[PacketLoss] RANDOM PACKET LOST -> "
                    + deviceId
                    + " FCNT="
                    + fields.get("FCNT")
                    + " probability="
                    + packetLossSimulator.getLossProbability());

            session.incrementRandomLosses(1);

            return;
        }
        
        
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
        
        
        
        session.incrementPacketsReceived();

        if (latencyMs >= 0) {

            session.addLatency(
                    latencyMs);
        }

        if (session.getLastFcnt() >= 0) {

            int expected =
                    session.getLastFcnt() + 1;

            if (currentFcnt > expected) {

                int gap =
                        currentFcnt - expected;

                System.out.println(
                        "[FCnt] Gap detectado en "
                        + deviceId
                        + ". Esperado="
                        + String.format("%04X", expected)
                        + " recibido="
                        + String.format("%04X", currentFcnt)
                        + " faltantes="
                        + gap
                        + " (no se suma a Lost para evitar doble conteo)");
            }
        }

        session.setLastFcnt(currentFcnt);

        System.out.println(
            "[Metrics] "
            + deviceId
            + " TxAttempts="
            + session.getPacketsTransmitted()
            + " OriginalMessages="
            + session.getOriginalMessages()
            + " Retransmissions="
            + session.getRetransmissionAttempts()
            + " Rx="
            + session.getPacketsReceived()
            + " Lost="
            + session.getPacketsLost()
            + " LinkBudgetLost="
            + session.getLinkBudgetLosses()
            + " RandomLost="
            + session.getRandomLosses());
        



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


        System.out.println(
                "[NetworkServer] Dispositivo identificado: "
                + deviceId);

        System.out.println(
                "[NetworkServer] Clase del dispositivo: "
                + device.getConfig().getDeviceClass());

        session.addReceivedBytes(
            payload.getBytes().length);
        
        session.registerPacketTimestamp();

        System.out.println(
            "[NetworkServer] Payload decodificado: "
            + context.getDecodedPayload());

        System.out.println(
            "[NetworkServer] Fuente decodificada: "
            + context.getDescription());


        session.setLastGatewayId(gatewayId);

        session.setLastRssi(
                result.getRxPowerDbm());

        session.addRssi(
                result.getRxPowerDbm());

        session.setLastSnr(
                result.getMarginDb());

        session.addSnr(
                result.getMarginDb());

        int recommendedSf =
                session.getCurrentSf();

        if (adrEnabled) {

            recommendedSf =
                    adrManager.recommendSpreadingFactor(
                            result.getMarginDb());

            if (recommendedSf != session.getCurrentSf()) {

                System.out.println(
                        "[ADR] Recomendación pendiente "
                        + device.getDeviceId()
                        + " -> SF"
                        + recommendedSf);
            }

            System.out.println(
                    "[ADR] "
                    + device.getDeviceId()
                    + " recomendado SF"
                    + recommendedSf);

        } else {

            System.out.println(
                    "[ADR] Desactivado para prueba. "
                    + device.getDeviceId()
                    + " mantiene SF"
                    + session.getCurrentSf());
        }

        session.setRecommendedSf(
                recommendedSf);

        System.out.println(
                "[DeviceSession] "
                + deviceId
                + " RSSI="
                + result.getRxPowerDbm()
                + " SNR="
                + result.getMarginDb());

        System.out.printf(
                "[Metrics] %s SNR Avg=%.2f dB%n",
                deviceId,
                session.getAverageSnr());

        System.out.println(
                "[Metrics] "
                + deviceId
                + " RSSI Avg="
                + String.format(
                        "%.2f",
                        session.getAverageRssi())
                + " dBm");

        System.out.printf(
                "[Metrics] %s Latency Avg=%.2f ms%n",
                deviceId,
                session.getAverageLatency());


            downlinkProcessor.process(
                    device,
                    session,
                    gateway,
                    context,
                    transport
            );
    
    }
    public DeviceSessionRegistry getSessionRegistry() {
        return sessionRegistry;
    }

    private boolean isLinkBudgetDrop(
            Device device,
            LinkBudgetResult result) {

        double receiverSensitivity =
                getReceiverSensitivity(
                        device.getSpreadingFactor());

        return result.getRxPowerDbm()
                < receiverSensitivity;
    }

       private double getReceiverSensitivity(int sf) {

        switch (sf) {

            case 7:
                return -123.0;

            case 8:
                return -126.0;

            case 9:
                return -129.0;

            case 10:
                return -132.0;

            case 11:
                return -134.5;

            case 12:
                return -137.0;

            default:
                return -123.0;
        }
    }

    public LinkBudgetResult registerTransmissionMetric(Device device, Gateway gateway, boolean los) {
        double dx = gateway.getX();
        double dy = gateway.getY();
        double distanceMeters = Math.sqrt(dx * dx + dy * dy);

        int sf =
            device.getSpreadingFactor();

        double receiverSensitivity =
                getReceiverSensitivity(sf);

        System.out.println(
            "[LinkBudget] "
            + device.getDeviceId()
            + " SF"
            + sf
            + " Sens="
            + receiverSensitivity
            + " dBm");

        LinkBudgetResult result = linkBudgetService.evaluate(
                device.getDeviceId(),
                gateway.getGatewayId(),
                distanceMeters,
                device.getConfig().getFrequencyMHz(),
                gateway.getMaxTxPowerDBm(),
                receiverSensitivity,
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