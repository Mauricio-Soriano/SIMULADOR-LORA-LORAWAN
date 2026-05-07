package mx.mauricio.lorawan.network.policy;

import java.util.Map;
import mx.mauricio.lorawan.device.Device;

public class ClassADownlinkPolicy implements DownlinkPolicy {

    @Override
    public DownlinkDecision evaluate(Device device, Map<String, String> fields, String transport) {
        String fPort = fields.get("FPORT");
        String data = fields.get("DATA");

        if ("1".equals(fPort) && isNumeric(data)) {
            double value = Double.parseDouble(data);
            if (value > 27.0) {
                return DownlinkDecision.send("CMD_ALERT_HIGH", "99");
            }
        }

        return DownlinkDecision.none();
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
