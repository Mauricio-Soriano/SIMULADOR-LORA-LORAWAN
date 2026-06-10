package mx.mauricio.lorawan.network.policy;

import java.util.Map;

import mx.mauricio.lorawan.device.Device;

public class ClassCDownlinkPolicy implements DownlinkPolicy {

    @Override
    public DownlinkDecision evaluate(
            Device device,
            Map<String, String> fields,
            String transport) {

        return DownlinkDecision.send(
                "CMD_CLASS_C_ACK",
                "99");
    }
}
