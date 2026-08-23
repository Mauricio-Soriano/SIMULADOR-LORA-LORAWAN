package mx.mauricio.lorawan.network;

import java.util.Map;

import mx.mauricio.lorawan.config.DeviceClass;
import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.frame.DownlinkFrame;
import mx.mauricio.lorawan.gateway.Gateway;
import mx.mauricio.lorawan.network.policy.ClassADownlinkPolicy;
import mx.mauricio.lorawan.network.policy.ClassBDownlinkPolicy;
import mx.mauricio.lorawan.network.policy.ClassCDownlinkPolicy;
import mx.mauricio.lorawan.network.policy.DownlinkDecision;
import mx.mauricio.lorawan.network.policy.DownlinkPolicy;

public class DownlinkProcessor {

    private final DownlinkPolicy classAPolicy =
            new ClassADownlinkPolicy();

    private final DownlinkPolicy classBPolicy =
            new ClassBDownlinkPolicy();

    private final DownlinkPolicy classCPolicy =
            new ClassCDownlinkPolicy();

    private final AckDownlinkBuilder ackBuilder =
            new AckDownlinkBuilder();

    public void process(
                Device device,
                DeviceSession session,
                Gateway gateway,
                UplinkContext context,
                String transport) {

        Map<String, String> fields =
                context.getFields();

        int uplinkFcnt =
                Integer.parseInt(
                        fields.get("FCNT"),
                        16);

        DownlinkPolicy policy =
                resolvePolicy(device);

        DownlinkDecision decision =
                policy.evaluate(device, fields, transport);

        boolean ackRequired =
                session.isAckRequired();

        String adrCommand =
                buildAdrCommand(session);

        if (!ackRequired
                && adrCommand == null
                && !decision.shouldSend()) {

                return;
        }

        String command =
                buildDownlinkCommand(
                        ackRequired,
                        adrCommand,
                        decision);

        String fPort =
                decision.shouldSend()
                        ? decision.getFPort()
                        : "99";

        String fCnt =
                String.format(
                        "%04X",
                        session.nextFCntDown());

        System.out.println(
                "[DeviceSession] "
                        + device.getDeviceId()
                        + " FCntDown="
                        + session.getFCntDown());

        if (ackRequired) {

        session.registerAckGenerated(
                uplinkFcnt);

        System.out.println(
                "[NetworkServer] ACK FCntDown="
                        + fCnt);

        System.out.println(
                "[NetworkServer] ACK LoRaWAN generado para "
                        + device.getDeviceId());

        System.out.println(
                "[ACK] "
                + device.getDeviceId()
                + " ACKGenerated="
                + session.getAckGenerated()
                + " ACKReceived="
                + session.getAckReceived()
                + " ACKLost="
                + session.getAckLost());
        }

        if (adrCommand != null) {

                System.out.println(
                        "[ADR] Downlink incluido -> "
                                + adrCommand);
        }

        if (decision.shouldSend()) {

                System.out.println(
                        "[NetworkServer] Comando incluido para "
                                + device.getDeviceId()
                                + ": "
                                + decision.getCommand());
        }

        DownlinkFrame downlink =
                new DownlinkFrame(
                        device.getDeviceId(),
                        fCnt,
                        fPort,
                        command,
                        ackRequired);

        gateway.sendDownlink(
                downlink.toPayloadString(),
                transport);

        if (ackRequired) {

                session.setAckRequired(false);
        }
        }


    private String buildAdrCommand(DeviceSession session) {

        if (session.getRecommendedSf() <= 0) {
                return null;
        }

        if (session.getRecommendedSf()
                == session.getCurrentSf()) {

                return null;
        }

        return "ADR:SF"
                + session.getRecommendedSf();
        }    

        private String buildDownlinkCommand(
                boolean ackRequired,
                String adrCommand,
                DownlinkDecision decision) {

        StringBuilder command =
                new StringBuilder();

        if (adrCommand != null) {

                command.append(adrCommand);
        }

        if (decision.shouldSend()) {

                if (command.length() > 0) {
                command.append(";");
                }

                command.append(
                        decision.getCommand());
        }

        if (command.length() == 0
                && ackRequired) {

                command.append("ACK");
        }

        return command.toString();
        }
    private DownlinkPolicy resolvePolicy(Device device) {

        DeviceClass deviceClass =
                device.getConfig().getDeviceClass();

        switch (deviceClass) {

            case CLASS_A:
                return classAPolicy;

            case CLASS_B:
                return classBPolicy;

            case CLASS_C:
                return classCPolicy;

            default:
                return (d, f, t) ->
                        DownlinkDecision.none();
        }
    }


}
