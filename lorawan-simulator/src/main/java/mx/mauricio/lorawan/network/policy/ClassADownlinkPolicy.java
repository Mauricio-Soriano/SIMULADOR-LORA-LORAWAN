package mx.mauricio.lorawan.network.policy;

import java.util.Map;

import mx.mauricio.lorawan.device.Device;

public class ClassADownlinkPolicy implements DownlinkPolicy {

    @Override
    public DownlinkDecision evaluate(
            Device device,
            Map<String, String> fields,
            String transport) {

        String fPort = fields.get("FPORT");
        String data = fields.get("DATA");

        if (!"1".equals(fPort) || data == null) {
            return DownlinkDecision.none();
        }

        try {

            String[] values = data.split(",");

            double temperature =
                    Double.parseDouble(values[0]);

            if (temperature > 27.0) {

                return DownlinkDecision.send(
                        "CMD_ALERT_HIGH",
                        "99");
            }

        } catch (Exception e) {

            System.out.println(
                "[ClassADownlinkPolicy] Error interpretando payload: "
                + data);
        }

        return DownlinkDecision.none();
    }
}