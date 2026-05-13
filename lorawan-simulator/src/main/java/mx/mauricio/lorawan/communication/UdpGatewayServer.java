package mx.mauricio.lorawan.communication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import mx.mauricio.lorawan.gateway.Gateway;

public class UdpGatewayServer implements Runnable {

    private final int port;
    private final Gateway gateway;
    private volatile boolean running = true;
    private DatagramSocket socket;

    public UdpGatewayServer(int port, Gateway gateway) {
        this.port = port;
        this.gateway = gateway;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port);
            byte[] buffer = new byte[2048];
            System.out.println("[UDP Server] Gateway escuchando en puerto " + port);

            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());
                    String senderIp = packet.getAddress().getHostAddress();
                    int senderPort = packet.getPort();

                    System.out.println("[UDP Server] Mensaje recibido desde " + senderIp + ":" + senderPort + ": " + message);
                    gateway.receiveUdpMessage(message, senderIp, senderPort, this);
                } catch (SocketException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            if (running) {
                e.printStackTrace();
            }
        }
    }

    public void sendDownlink(String payload, String ip, int port) {
        try {
            byte[] data = payload.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    java.net.InetAddress.getByName(ip),
                    port
            );
            socket.send(packet);
            System.out.println("[UDP Server] Downlink enviado a " + ip + ":" + port + " -> " + payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}