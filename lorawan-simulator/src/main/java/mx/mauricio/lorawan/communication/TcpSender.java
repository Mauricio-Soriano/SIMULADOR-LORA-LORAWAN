package mx.mauricio.lorawan.communication;

import java.io.PrintWriter;
import java.net.Socket;

public class TcpSender {
    private final String host;
    private final int port;

    public TcpSender(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void send(String message) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

