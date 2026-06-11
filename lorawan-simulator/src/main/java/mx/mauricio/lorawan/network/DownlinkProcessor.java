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

        if (session.isAckRequired()) {

            DownlinkFrame ackFrame =
                    ackBuilder.buildAck(
                            device,
                            session);

            System.out.println(
                    "[NetworkServer] ACK LoRaWAN generado para "
                            + device.getDeviceId());

            gateway.sendDownlink(
                    ackFrame.toPayloadString(),
                    transport);

            session.setAckRequired(false);
        }

        DownlinkPolicy policy =
                resolvePolicy(device);

        DownlinkDecision decision =
                policy.evaluate(device, fields, transport);

        if (!decision.shouldSend()) {
            return;
        }

        send(
                device,
                session,
                gateway,
                decision,
                transport
        );
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

    private void send(
            Device device,
            DeviceSession session,
            Gateway gateway,
            DownlinkDecision decision,
            String transport) {

        String fCnt =
                String.format(
                        "%04X",
                        session.nextFCntDown());

        System.out.println(
                "[DeviceSession] "
                        + device.getDeviceId()
                        + " FCntDown="
                        + session.getFCntDown());

        DownlinkFrame downlink =
            new DownlinkFrame(
                    device.getDeviceId(),
                    fCnt,
                    decision.getFPort(),
                    decision.getCommand(),
                    false);

        String payload =
                downlink.toPayloadString();

        System.out.println(
                "[NetworkServer] Generando Downlink para "
                        + device.getDeviceId()
                        + ": "
                        + decision.getCommand());

        gateway.sendDownlink(
                payload,
                transport);
    }
}
