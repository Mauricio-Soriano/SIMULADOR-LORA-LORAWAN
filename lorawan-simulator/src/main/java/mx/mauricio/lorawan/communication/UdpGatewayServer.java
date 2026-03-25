package mx.mauricio.lorawan.communication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import mx.mauricio.lorawan.gateway.Gateway;

public class UdpGatewayServer implements Runnable {
    private final int port;
    private final Gateway gateway;
    private boolean running = true;

    public UdpGatewayServer(int port, Gateway gateway) {
        this.port = port;
        this.gateway = gateway;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[2048];
            System.out.println("[UDP Server] Gateway escuchando en puerto " + port);

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                String senderIp = packet.getAddress().getHostAddress();

                System.out.println("[UDP Server] Mensaje recibido desde " + senderIp + ": " + message);

                gateway.receiveUdpMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
    }
}
