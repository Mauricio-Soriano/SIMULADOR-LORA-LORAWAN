package mx.mauricio.lorawan.network;

public class DuplicateFrameDetector {

    public boolean isDuplicate(
            DeviceSession session,
            String fCntHex) {

        int receivedFCnt =
                Integer.parseInt(
                        fCntHex,
                        16);

        if (receivedFCnt
                <= session.getLastFCntUpReceived()) {

            return true;
        }

        session.setLastFCntUpReceived(
                receivedFCnt);

        return false;
    }
}
