package mx.mauricio.lorawan.communication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TcpSender {
    private final String host;
    private final int port;

    public TcpSender(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String send(String message) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(message);

            String response = in.readLine();
            if (response != null) {
                System.out.println("[TcpSender] Downlink TCP recibido: " + response);
            } else {
                System.out.println("[TcpSender] No se recibió respuesta TCP.");
            }

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
