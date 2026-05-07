package mx.mauricio.lorawan.network.policy;

import java.util.Map;
import mx.mauricio.lorawan.device.Device;

public interface DownlinkPolicy {
    DownlinkDecision evaluate(Device device, Map<String, String> fields, String transport);
}
