package mx.mauricio.lorawan.performance;

public class Cost231WalfischIkegamiModel {

    public double calculateFreeSpaceLossDb(
            double distanceMeters,
            double frequencyMHz) {

        double distanceKm =
                Math.max(distanceMeters / 1000.0, 0.001);

        return 32.45
                + 20.0 * Math.log10(distanceKm)
                + 20.0 * Math.log10(frequencyMHz);
    }

    public double calculateLrtsDb(
            double streetWidthMeters,
            double frequencyMHz,
            double hbMeters,
            double hrMeters,
            double loriDb) {

        double w =
                Math.max(streetWidthMeters, 1.0);

        double hb =
                Math.max(hbMeters, 0.1);

        double hr =
                Math.max(hrMeters, 0.1);

        double heightDifference =
                Math.max(hb - hr, 0.1);

        return -16.9
                - 10.0 * Math.log10(w)
                + 10.0 * Math.log10(frequencyMHz)
                + 20.0 * Math.log10(heightDifference)
                + loriDb;
    }

    public double calculateLmsdDb(
            double buildingSeparationMeters,
            double frequencyMHz,
            double distanceMeters,
            double hbMeters,
            double hrMeters,
            double kaDb,
            double kdDb,
            double kfDb,
            double lbshDb) {

        double b =
                Math.max(buildingSeparationMeters, 1.0);

        double distanceKm =
                Math.max(distanceMeters / 1000.0, 0.001);

        return lbshDb
                + kaDb
                + kdDb * Math.log10(distanceKm)
                + kfDb * Math.log10(frequencyMHz)
                - 9.0 * Math.log10(b);
    }

    public double calculateLoSPathLossDb(
            double distanceMeters,
            double frequencyMHz) {

        double distanceKm =
                Math.max(distanceMeters / 1000.0, 0.001);

        return 42.64
                + 26.0 * Math.log10(distanceKm)
                + 20.0 * Math.log10(frequencyMHz);
    }

    public double calculateNLoSPathLossDb(
            double distanceMeters,
            double frequencyMHz,
            double streetWidthMeters,
            double hbMeters,
            double hrMeters,
            double loriDb,
            double buildingSeparationMeters,
            double kaDb,
            double kdDb,
            double kfDb,
            double lbshDb) {

        double l0 =
                calculateFreeSpaceLossDb(
                        distanceMeters,
                        frequencyMHz);

        double lrts =
                calculateLrtsDb(
                        streetWidthMeters,
                        frequencyMHz,
                        hbMeters,
                        hrMeters,
                        loriDb);

        double lmsd =
                calculateLmsdDb(
                        buildingSeparationMeters,
                        frequencyMHz,
                        distanceMeters,
                        hbMeters,
                        hrMeters,
                        kaDb,
                        kdDb,
                        kfDb,
                        lbshDb);

        double additionalLoss =
                lrts + lmsd;

        if (additionalLoss < 0.0) {
            additionalLoss = 0.0;
        }

        return l0 + additionalLoss;
    }

    public double calculatePathLossDb(
            boolean los,
            double distanceMeters,
            double frequencyMHz,
            double streetWidthMeters,
            double hbMeters,
            double hrMeters,
            double loriDb,
            double buildingSeparationMeters,
            double kaDb,
            double kdDb,
            double kfDb,
            double lbshDb) {

        return los
                ? calculateLoSPathLossDb(
                        distanceMeters,
                        frequencyMHz)
                : calculateNLoSPathLossDb(
                        distanceMeters,
                        frequencyMHz,
                        streetWidthMeters,
                        hbMeters,
                        hrMeters,
                        loriDb,
                        buildingSeparationMeters,
                        kaDb,
                        kdDb,
                        kfDb,
                        lbshDb);
    }
}