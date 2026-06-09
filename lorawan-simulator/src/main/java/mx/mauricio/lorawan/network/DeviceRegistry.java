package mx.mauricio.lorawan.network;

import java.util.HashMap;
import java.util.Map;

import mx.mauricio.lorawan.device.Device;

public class DeviceRegistry {

    private final Map<String, Device> devices = new HashMap<>();

    public void register(Device device) {

        devices.put(device.getDeviceId(), device);

        System.out.println(
                "[DeviceRegistry] Dispositivo registrado: "
                + device.getDeviceId()
                + " clase="
                + device.getConfig().getDeviceClass()
        );
    }

    public Device get(String deviceId) {
        return devices.get(deviceId);
    }

    public boolean contains(String deviceId) {
        return devices.containsKey(deviceId);
    }

    public int size() {
        return devices.size();
    }

    public Map<String, Device> getAll() {
        return Map.copyOf(devices);
    }
}
