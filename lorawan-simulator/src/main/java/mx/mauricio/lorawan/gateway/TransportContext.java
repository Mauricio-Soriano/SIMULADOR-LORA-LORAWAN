package mx.mauricio.lorawan.gateway;

import java.net.Socket;
import mx.mauricio.lorawan.communication.UdpGatewayServer;

public class TransportContext {

    public enum Type {
        UDP, TCP, NONE
    }

    private final Type type;
    private final String senderIp;
    private final int senderPort;
    private final UdpGatewayServer udpServer;
    private final Socket tcpSocket;

    private TransportContext(Type type, String senderIp, int senderPort,
                             UdpGatewayServer udpServer, Socket tcpSocket) {
        this.type = type;
        this.senderIp = senderIp;
        this.senderPort = senderPort;
        this.udpServer = udpServer;
        this.tcpSocket = tcpSocket;
    }

    public static TransportContext udp(String senderIp, int senderPort, UdpGatewayServer udpServer) {
        return new TransportContext(Type.UDP, senderIp, senderPort, udpServer, null);
    }

    public static TransportContext tcp(String senderIp, Socket tcpSocket) {
        return new TransportContext(Type.TCP, senderIp, -1, null, tcpSocket);
    }

    public static TransportContext none() {
        return new TransportContext(Type.NONE, null, -1, null, null);
    }

    public Type getType() { return type; }
    public String getSenderIp() { return senderIp; }
    public int getSenderPort() { return senderPort; }
    public UdpGatewayServer getUdpServer() { return udpServer; }
    public Socket getTcpSocket() { return tcpSocket; }

    public boolean isUdp() { return type == Type.UDP; }
    public boolean isTcp() { return type == Type.TCP; }
}
