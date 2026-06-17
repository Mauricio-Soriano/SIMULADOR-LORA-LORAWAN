package mx.mauricio.lorawan.network;

public class AdrManager {

    public int recommendSpreadingFactor(
            double rssi) {

        if (rssi > -110) {
            return 7;
        }

        if (rssi > -120) {
            return 8;
        }

        if (rssi > -130) {
            return 9;
        }

        return 10;
    }
}