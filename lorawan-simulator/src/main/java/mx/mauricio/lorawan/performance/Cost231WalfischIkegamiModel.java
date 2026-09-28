package mx.mauricio.lorawan.performance;

import mx.mauricio.lorawan.performance.Cost231LinkBudgetParameters.UrbanEnvironment;

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

    public double calculateLoSPathLossDb(
            double distanceMeters,
            double frequencyMHz) {

        double distanceKm =
                Math.max(distanceMeters / 1000.0, 0.001);

        return 42.64
                + 26.0 * Math.log10(distanceKm)
                + 20.0 * Math.log10(frequencyMHz);
    }

    // -----------------------------------------------------------------
    // COST231 Walfisch-Ikegami completo a partir de parámetros físicos
    // -----------------------------------------------------------------

    public double calculateStreetOrientationLossDb(
            double streetOrientationDegrees) {

        double alpha = streetOrientationDegrees;

        if (alpha <= 0.0 || alpha >= 90.0) {
            throw new IllegalArgumentException(
                    "streetOrientationDegrees debe estar entre 0 y 90 grados");
        }

        /*
         * El artículo usa intervalos abiertos y no define explícitamente
         * alpha = 35 ni alpha = 55. Para evitar huecos numéricos se adopta:
         * 0 < alpha < 35, 35 <= alpha < 55 y 55 <= alpha < 90.
         */
        if (alpha < 35.0) {
            return -10.0 + 0.35 * alpha;
        }

        if (alpha < 55.0) {
            return 2.5 + 0.0755 * (alpha - 35.0);
        }

        return 4.0 - 0.0114 * (alpha - 55.0);
    }

    public double calculateBaseStationHeightLossDb(
            double baseStationHeightMeters,
            double averageBuildingHeightMeters) {

        if (baseStationHeightMeters
                > averageBuildingHeightMeters) {

            return -18.0 * Math.log10(
                    1.0
                    + baseStationHeightMeters
                    - averageBuildingHeightMeters);
        }

        return 0.0;
    }

    public double calculateKaDb(
            double baseStationHeightMeters,
            double averageBuildingHeightMeters,
            double distanceMeters) {

        double distanceKm =
                Math.max(distanceMeters / 1000.0, 0.001);

        // El artículo no define ht == hb para Ka; se adopta Ka = 54.
        if (baseStationHeightMeters
                >= averageBuildingHeightMeters) {
            return 54.0;
        }

        if (distanceKm >= 0.5) {
            return 54.0
                    - 0.8
                    * (baseStationHeightMeters
                    - averageBuildingHeightMeters);
        }

        return 54.0
                - 1.6
                * (baseStationHeightMeters
                - averageBuildingHeightMeters)
                * distanceKm;
    }

    public double calculateKdDb(
            double baseStationHeightMeters,
            double averageBuildingHeightMeters) {

        if (baseStationHeightMeters
                > averageBuildingHeightMeters) {
            return 18.0;
        }

        return 18.0
                - 15.0
                * ((baseStationHeightMeters
                - averageBuildingHeightMeters)
                / averageBuildingHeightMeters);
    }

    public double calculateKfDb(
            double frequencyMHz,
            UrbanEnvironment urbanEnvironment) {

        double normalizedFrequency =
                (frequencyMHz / 925.0) - 1.0;

        if (urbanEnvironment
                == UrbanEnvironment.MEDIUM_SIZE_CITY) {

            return -4.0
                    + 0.7 * normalizedFrequency;
        }

        if (urbanEnvironment
                == UrbanEnvironment.DOWNTOWN) {

            // Se implementa literalmente la ecuación impresa en el artículo.
            return 1.5 * normalizedFrequency;
        }

        throw new IllegalArgumentException(
                "urbanEnvironment no soportado");
    }

    public double calculateLrtsFromPhysicalParametersDb(
            double streetWidthMeters,
            double frequencyMHz,
            double averageBuildingHeightMeters,
            double mobileStationHeightMeters,
            double streetOrientationDegrees) {

        double w =
                Math.max(streetWidthMeters, 1.0);

        double heightDifference =
                averageBuildingHeightMeters
                - mobileStationHeightMeters;

        if (heightDifference <= 0.0) {
            throw new IllegalArgumentException(
                    "averageBuildingHeightMeters debe ser mayor "
                    + "que mobileStationHeightMeters para calcular Lrts");
        }

        double loriDb =
                calculateStreetOrientationLossDb(
                        streetOrientationDegrees);

        return -16.9
                - 10.0 * Math.log10(w)
                + 10.0 * Math.log10(frequencyMHz)
                + 20.0 * Math.log10(heightDifference)
                + loriDb;
    }

    public double calculateLmsdFromPhysicalParametersDb(
            double buildingSeparationMeters,
            double frequencyMHz,
            double distanceMeters,
            double baseStationHeightMeters,
            double averageBuildingHeightMeters,
            UrbanEnvironment urbanEnvironment) {

        double b =
                Math.max(buildingSeparationMeters, 1.0);

        double distanceKm =
                Math.max(distanceMeters / 1000.0, 0.001);

        double lbshDb =
                calculateBaseStationHeightLossDb(
                        baseStationHeightMeters,
                        averageBuildingHeightMeters);

        double kaDb =
                calculateKaDb(
                        baseStationHeightMeters,
                        averageBuildingHeightMeters,
                        distanceMeters);

        double kdDb =
                calculateKdDb(
                        baseStationHeightMeters,
                        averageBuildingHeightMeters);

        double kfDb =
                calculateKfDb(
                        frequencyMHz,
                        urbanEnvironment);

        return lbshDb
                + kaDb
                + kdDb * Math.log10(distanceKm)
                + kfDb * Math.log10(frequencyMHz)
                - 9.0 * Math.log10(b);
    }

    public double calculateNLoSPathLossFromPhysicalParametersDb(
            double distanceMeters,
            double frequencyMHz,
            double streetWidthMeters,
            double baseStationHeightMeters,
            double averageBuildingHeightMeters,
            double mobileStationHeightMeters,
            double streetOrientationDegrees,
            double buildingSeparationMeters,
            UrbanEnvironment urbanEnvironment) {

        double l0 =
                calculateFreeSpaceLossDb(
                        distanceMeters,
                        frequencyMHz);

        double lrts =
                calculateLrtsFromPhysicalParametersDb(
                        streetWidthMeters,
                        frequencyMHz,
                        averageBuildingHeightMeters,
                        mobileStationHeightMeters,
                        streetOrientationDegrees);

        double lmsd =
                calculateLmsdFromPhysicalParametersDb(
                        buildingSeparationMeters,
                        frequencyMHz,
                        distanceMeters,
                        baseStationHeightMeters,
                        averageBuildingHeightMeters,
                        urbanEnvironment);

        // Ruta nueva basada literalmente en P_NLoS = L0 + Lrts + Lmsd.
        return l0 + lrts + lmsd;
    }

    public double calculatePathLossDb(
            double distanceMeters,
            Cost231LinkBudgetParameters parameters) {

        if (parameters == null) {
            parameters = new Cost231LinkBudgetParameters();
        }

        parameters.validate();

        if (parameters.isLos()) {
            return calculateLoSPathLossDb(
                    distanceMeters,
                    parameters.getFrequencyMHz());
        }

        return calculateNLoSPathLossFromPhysicalParametersDb(
                distanceMeters,
                parameters.getFrequencyMHz(),
                parameters.getStreetWidthMeters(),
                parameters.getBaseStationHeightMeters(),
                parameters.getAverageBuildingHeightMeters(),
                parameters.getMobileStationHeightMeters(),
                parameters.getStreetOrientationDegrees(),
                parameters.getBuildingSeparationMeters(),
                parameters.getUrbanEnvironment());
    }

    // -----------------------------------------------------------------
    // API legacy temporal: retirar tras adaptar LinkBudgetService (6.2.4)
    // -----------------------------------------------------------------

    @Deprecated
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

    @Deprecated
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

    @Deprecated
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

    @Deprecated
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