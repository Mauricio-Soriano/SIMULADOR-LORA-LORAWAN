package mx.mauricio.lorawan.gateway;

import java.io.PrintWriter;
import java.net.Socket;

import mx.mauricio.lorawan.communication.UdpGatewayServer;
import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.network.NetworkServer;

public class Gateway {

    private final String gatewayId;
    private final NetworkServer networkServer;

    private final double x, y;
    private final int maxTxPowerDBm;

    private String lastUdpSenderIp;
    private int lastUdpSenderPort;
    private UdpGatewayServer lastUdpServer;

    private String lastTcpSenderIp;
    private Socket lastTcpClientSocket;

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

        networkServer.receiveFromGateway(gatewayId, payload, "UDP");
    }

    public void receiveTcpMessage(String payload, String senderIp, Socket clientSocket) {
        System.out.printf("[Gateway %s @%.1f,%.1f] Rx TCP payload desde %s%n",
                gatewayId, x, y, senderIp);

        this.lastTcpSenderIp = senderIp;
        this.lastTcpClientSocket = clientSocket;

        networkServer.receiveFromGateway(gatewayId, payload, "TCP");
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

    public void sendDownlink(String payload, String transport) {
        System.out.println("[Gateway " + gatewayId + "] Transmitiendo Downlink...");

        if ("UDP".equalsIgnoreCase(transport)) {
            if (lastUdpServer != null && lastUdpSenderIp != null && lastUdpSenderPort > 0) {
                lastUdpServer.sendDownlink(payload, lastUdpSenderIp, lastUdpSenderPort);
                return;
            }
            System.out.println("[Gateway " + gatewayId + "] No hay contexto UDP disponible para responder.");
            return;
        }

        if ("TCP".equalsIgnoreCase(transport)) {
            if (lastTcpClientSocket != null && !lastTcpClientSocket.isClosed()) {
                try {
                    PrintWriter out = new PrintWriter(lastTcpClientSocket.getOutputStream(), true);
                    out.println(payload);
                    System.out.println("[Gateway " + gatewayId + "] Downlink TCP enviado a " + lastTcpSenderIp + " -> " + payload);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            System.out.println("[Gateway " + gatewayId + "] No hay contexto TCP disponible para responder.");
            return;
        }

        System.out.println("[Gateway " + gatewayId + "] Transporte no soportado: " + transport);
    }
}