package mx.mauricio.lorawan.device;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import mx.mauricio.lorawan.communication.TcpSender;
import mx.mauricio.lorawan.config.DeviceClass;
import mx.mauricio.lorawan.config.LoRaConfig;
import mx.mauricio.lorawan.frame.ApplicationPayload;
import mx.mauricio.lorawan.frame.UplinkFrame;
import mx.mauricio.lorawan.gateway.Gateway;

public class Device {
    private final String deviceId;
    private final Gateway gateway;
    private final LoRaConfig config;
    private int frameCounter = 0;

    public Device(String deviceId, Gateway gateway, LoRaConfig config) {
        this.deviceId = deviceId;
        this.gateway = gateway;
        this.config = config;
    }

    public Device(String deviceId, Gateway gateway) {
        this(deviceId, gateway, LoRaConfig.US915_CLASS_A);
    }

    public void sendUplink(ApplicationPayload appPayload) {
        frameCounter++;
        UplinkFrame frame = new UplinkFrame(this, appPayload);

        System.out.printf("[Device %s %s SF%d] %s%n",
                deviceId,
                config.getDeviceClass(),
                config.getSpreadingFactor(),
                frame);

        String payload = frame.toHexString();

        if (config.getDeviceClass() == DeviceClass.CLASS_A) {
            sendUdpAndWaitResponse(payload);
        } else {
            TcpSender tcpSender = new TcpSender("127.0.0.1", 6000);
            tcpSender.send(payload);
        }
    }

    private void sendUdpAndWaitResponse(String payload) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(3000);

            byte[] sendData = payload.getBytes();
            InetAddress gatewayAddress = InetAddress.getByName("127.0.0.1");

            DatagramPacket sendPacket = new DatagramPacket(
                    sendData,
                    sendData.length,
                    gatewayAddress,
                    5000
            );

            socket.send(sendPacket);
            System.out.println("[Device " + deviceId + "] Uplink UDP enviado al gateway: " + payload);

            byte[] buffer = new byte[2048];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

            socket.receive(responsePacket);

            String downlink = new String(responsePacket.getData(), 0, responsePacket.getLength());
            System.out.println("[Device " + deviceId + "] Downlink UDP recibido: " + downlink);

        } catch (SocketTimeoutException e) {
            System.out.println("[Device " + deviceId + "] No se recibió downlink UDP dentro del tiempo de espera.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getDeviceId() { return deviceId; }
    public LoRaConfig getConfig() { return config; }
    public int getFrameCounter() { return frameCounter; }
    public int getSpreadingFactor() { return config.getSpreadingFactor(); }
}