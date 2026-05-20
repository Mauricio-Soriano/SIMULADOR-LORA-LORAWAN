package mx.mauricio.lorawan.communication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class TcpSender {

    private final String host;
    private final int port;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public TcpSender(String host, int port) {
        this(host, port, 2000, 2000);
    }

    public TcpSender(String host, int port, int connectTimeoutMs, int readTimeoutMs) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public String send(String message) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);

            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                out.println(message);
                return in.readLine();
            }
        } catch (SocketTimeoutException e) {
            System.out.println("[TcpSender] Timeout esperando respuesta TCP.");
            return null;
        } catch (ConnectException e) {
            System.out.println("[TcpSender] No se pudo conectar al gateway TCP: " + host + ":" + port);
            return null;
        } catch (Exception e) {
            System.out.println("[TcpSender] Error TCP: " + e.getMessage());
            return null;
        }
    }
}