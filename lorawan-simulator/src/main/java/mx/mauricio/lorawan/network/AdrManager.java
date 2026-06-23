package mx.mauricio.lorawan.network;

public class AdrManager {

public int recommendSpreadingFactor(
            double marginDb) {

        if (marginDb > 20) {
            return 7;
        }

        if (marginDb > 10) {
            return 8;
        }

        if (marginDb > 5) {
            return 9;
        }

        if (marginDb > 0) {
            return 10;
        }

        if (marginDb > -5) {
            return 11;
        }

        return 12;
    }
}