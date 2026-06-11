package mx.mauricio.lorawan.network;

import mx.mauricio.lorawan.device.Device;
import mx.mauricio.lorawan.frame.DownlinkFrame;

public class AckDownlinkBuilder {

    public DownlinkFrame buildAck(
            Device device,
            DeviceSession session) {

        String fCnt =
                String.format(
                        "%04X",
                        session.nextFCntDown());

        return new DownlinkFrame(
            device.getDeviceId(),
            fCnt,
            "99",
            "ACK",
            true);
    }
}
