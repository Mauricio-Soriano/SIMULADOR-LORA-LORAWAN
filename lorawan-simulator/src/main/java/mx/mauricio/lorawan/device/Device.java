package mx.mauricio.lorawan.device;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Map;

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
    private boolean confirmed;
    private int frameCounter = 0;
    private static final int MAX_RETRIES = 3;
    private int retryCount = 0;
    private boolean ackReceived = false;

    public Device(
            String deviceId,
            Gateway gateway,
            LoRaConfig config,
            boolean confirmed) {

        this.deviceId = deviceId;
        this.gateway = gateway;
        this.config = config;
        this.confirmed = confirmed;
    }

    public Device(
            String deviceId,
            Gateway gateway,
            LoRaConfig config) {

        this(deviceId, gateway, config, false);
    }




    private void retransmit(
            String payload,
            TcpSender sender) {

        while (!ackReceived
                && retryCount < MAX_RETRIES) {

            retryCount++;

            System.out.println(
                    "[Device "
                            + deviceId
                            + "] Retransmisión #"
                            + retryCount);

            String response =
                    sender.send(payload);

            if (response != null
                    && response.contains("ACK=true")) {

                ackReceived = true;

                System.out.println(
                        "[Device "
                                + deviceId
                                + "] ACK recibido correctamente");

                break;
            }
        }
        

        if (!ackReceived) {

            System.out.println(
                    "[Device "
                            + deviceId
                            + "] ACK no recibido tras "
                            + MAX_RETRIES
                            + " intentos");
        }
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

            ackReceived = false;
            retryCount = 0;

            sendUdpAndWaitResponse(payload);

        } else {

            TcpSender tcpSender =
                    new TcpSender("127.0.0.1", 6000);

            String downlink =
                    tcpSender.send(payload);

            if (downlink != null) {

                System.out.println(
                        "[Device " + deviceId +
                        "] Downlink TCP recibido: "
                        + downlink);

                if (downlink.contains("ACK=true")) {

                    ackReceived = true;

                    System.out.println(
                            "[Device "
                            + deviceId
                            + "] ACK recibido correctamente");

                }
                if (confirmed && !ackReceived) {

                    retransmit(
                            payload,
                            tcpSender);
                }
            }
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

            String downlink =
                    new String(
                            responsePacket.getData(),
                            0,
                            responsePacket.getLength());

            System.out.println(
                    "[Device "
                            + deviceId
                            + "] Downlink UDP recibido: "
                            + downlink);

            Map<String, String> fields =
                    new java.util.HashMap<>();

            for (String part : downlink.split("\\|")) {

                String[] kv =
                        part.split("=", 2);

                if (kv.length == 2) {

                    fields.put(
                            kv[0],
                            kv[1]);
                }
            }

            boolean ackReceived =
                    "true".equals(
                            fields.get("ACK"));

            if (ackReceived) {

                System.out.println(
                        "[Device "
                                + deviceId
                                + "] ACK recibido del Network Server");
            }

        } catch (SocketTimeoutException e) {
            System.out.println("[Device " + deviceId + "] No se recibió downlink UDP dentro del tiempo de espera.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean isConfirmed() {
        return confirmed;
    }
    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public String getDeviceId() { return deviceId; }
    public LoRaConfig getConfig() { return config; }
    public int getFrameCounter() { return frameCounter; }
    public int getSpreadingFactor() { return config.getSpreadingFactor(); }
}