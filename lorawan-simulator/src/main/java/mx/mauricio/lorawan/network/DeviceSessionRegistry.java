package mx.mauricio.lorawan.network;

import java.util.HashMap;
import java.util.Map;

public class DeviceSessionRegistry {

    private final Map<String, DeviceSession> sessions =
            new HashMap<>();

    public DeviceSession getOrCreate(String deviceId) {

        return sessions.computeIfAbsent(
                deviceId,
                DeviceSession::new
        );
    }

    public DeviceSession get(String deviceId) {
        return sessions.get(deviceId);
    }

    public boolean contains(String deviceId) {
        return sessions.containsKey(deviceId);
    }

    public int size() {
        return sessions.size();
    }
}