package mx.mauricio.lorawan.network.policy;

import java.util.Map;
import mx.mauricio.lorawan.device.Device;

public class ClassBDownlinkPolicy implements DownlinkPolicy {

    @Override
    public DownlinkDecision evaluate(Device device, Map<String, String> fields, String transport) {
        String fPort = fields.get("FPORT");

        if ("2".equals(fPort)) {
            return DownlinkDecision.send("CMD_TCP_ACK", "99");
        }

        return DownlinkDecision.none();
    }
}