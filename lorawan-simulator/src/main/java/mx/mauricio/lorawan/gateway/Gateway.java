package mx.mauricio.lorawan.gateway;

import mx.mauricio.lorawan.communication.UdpGatewayServer;
import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.network.NetworkServer;

public class Gateway {

    private final String gatewayId;
    private final NetworkServer networkServer;

    // Parámetros del Gateway
    private final double x, y;
    private final int maxTxPowerDBm;

    // Contexto UDP para responder al último emisor
    private String lastUdpSenderIp;
    private int lastUdpSenderPort;
    private UdpGatewayServer lastUdpServer;

    public Gateway(String gatewayId, NetworkServer networkServer) {
        this(gatewayId, networkServer, 0.0, 0.0, 20);
    }

    public Gateway(String gatewayId, NetworkServer networkServer,
                   double x, double y, int maxTxPowerDBm) {
        this.gatewayId = gatewayId;
        this.networkServer = networkServer;
        this.x = x;
        this.y = y;
        this.maxTxPowerDBm = maxTxPowerDBm;
    }

    public void receiveUdpMessage(String payload, String senderIp, int senderPort, UdpGatewayServer udpServer) {
        System.out.printf("[Gateway %s @%.1f,%.1f] Rx UDP payload desde %s:%d%n",
                gatewayId, x, y, senderIp, senderPort);

        this.lastUdpSenderIp = senderIp;
        this.lastUdpSenderPort = senderPort;
        this.lastUdpServer = udpServer;

        networkServer.receiveFromGateway(gatewayId, payload);
    }

    public void receiveTcpMessage(String payload) {
        System.out.printf("[Gateway %s @%.1f,%.1f] Rx TCP payload%n", gatewayId, x, y);
        networkServer.receiveFromGateway(gatewayId, payload);
    }

    public String getGatewayId() { return gatewayId; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getMaxTxPowerDBm() { return maxTxPowerDBm; }

    public void receiveFromDevice(Device device, String payload) {
        System.out.println("[Gateway " + gatewayId + " @" + x + "," + y
                + "] Rx from " + device.getDeviceId()
                + " (SF" + device.getSpreadingFactor() + ")");
        networkServer.handleUplink(device, this, payload);
    }

    public void sendDownlink(String payload) {
        System.out.println("[Gateway " + gatewayId + "] Transmitiendo Downlink...");

        if (lastUdpServer == null || lastUdpSenderIp == null || lastUdpSenderPort <= 0) {
            System.out.println("[Gateway " + gatewayId + "] No hay contexto UDP disponible para responder.");
            return;
        }

        lastUdpServer.sendDownlink(payload, lastUdpSenderIp, lastUdpSenderPort);
    }
}



