package mx.mauricio.lorawan.communication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import mx.mauricio.lorawan.gateway.Gateway;

public class TcpGatewayServer implements Runnable {

    private final int port;
    private final Gateway gateway;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public TcpGatewayServer(int port, Gateway gateway) {
        this.port = port;
        this.gateway = gateway;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[TCP Server] Gateway escuchando en puerto " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()));

                    String message = in.readLine();
                    String senderIp = clientSocket.getInetAddress().getHostAddress();

                    if (message != null) {
                        System.out.println("[TCP Server] Mensaje recibido desde " + senderIp + ": " + message);
                        gateway.receiveTcpMessage(message, senderIp, clientSocket);
                    }

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

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}