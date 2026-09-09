package mx.mauricio.lorawan.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

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
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));

            System.out.println(
                    "[TCP Server] Gateway escuchando en puerto "
                    + port);

            while (running) {
                try {
                    Socket clientSocket =
                            serverSocket.accept();

                    handleClient(
                            clientSocket);

                } catch (SocketException e) {

                    if (running) {
                        System.out.println(
                                "[TCP Server] Socket cerrado inesperadamente: "
                                + e.getMessage());
                    }

                } catch (Exception e) {

                    if (running) {
                        System.out.println(
                                "[TCP Server] Error atendiendo cliente: "
                                + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {

            if (running) {
                System.out.println(
                        "[TCP Server] Error iniciando servidor: "
                        + e.getMessage());
            }

        } finally {

            closeServerSocket();
        }
    }

    private void handleClient(
            Socket clientSocket) {

        try (Socket socket = clientSocket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(
                             socket.getInputStream(),
                             StandardCharsets.UTF_8))) {

            String message =
                    in.readLine();

            String senderIp =
                    socket
                            .getInetAddress()
                            .getHostAddress();

            if (message != null) {

                System.out.println(
                        "[TCP Server] Mensaje recibido desde "
                        + senderIp
                        + ": "
                        + message);

                gateway.receiveTcpMessage(
                        message,
                        senderIp,
                        socket);
            }

        } catch (Exception e) {

            if (running) {
                System.out.println(
                        "[TCP Server] Error procesando cliente: "
                        + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        closeServerSocket();
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null
                    && !serverSocket.isClosed()) {

                serverSocket.close();
            }

        } catch (IOException e) {

            System.out.println(
                    "[TCP Server] Error cerrando ServerSocket: "
                    + e.getMessage());
        }
    }
}