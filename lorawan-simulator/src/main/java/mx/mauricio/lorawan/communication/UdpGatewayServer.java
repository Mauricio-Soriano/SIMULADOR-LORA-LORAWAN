package mx.mauricio.lorawan.communication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import mx.mauricio.lorawan.gateway.Gateway;

public class UdpGatewayServer implements Runnable {

    private final int port;
    private final Gateway gateway;
    private boolean running = true;
    private DatagramSocket socket;

    public UdpGatewayServer(int port, Gateway gateway) {
        this.port = port;
        this.gateway = gateway;
    }

    @Override
    public void run() {
        try (DatagramSocket serverSocket = new DatagramSocket(port)) {
            this.socket = serverSocket;
            byte[] buffer = new byte[2048];
            System.out.println("[UDP Server] Gateway escuchando en puerto " + port);

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                String senderIp = packet.getAddress().getHostAddress();
                int senderPort = packet.getPort();

                System.out.println("[UDP Server] Mensaje recibido desde "
                        + senderIp + ":" + senderPort + ": " + message);

                gateway.receiveUdpMessage(message, senderIp, senderPort, this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendDownlink(String payload, String destinationIp, int destinationPort) {
        try {
            if (socket == null) {
                System.out.println("[UDP Server] Socket no disponible para downlink.");
                return;
            }

            byte[] data = payload.getBytes();
            InetAddress address = InetAddress.getByName(destinationIp);
            DatagramPacket response = new DatagramPacket(data, data.length, address, destinationPort);
            socket.send(response);

            System.out.println("[UDP Server] Downlink enviado a "
                    + destinationIp + ":" + destinationPort + " -> " + payload);
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


// modificacion para que pueda contestar el gateway Actividad 17.9
/* 
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
*/