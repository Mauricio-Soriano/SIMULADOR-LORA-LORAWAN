package mx.mauricio.lorawan.performance;

public class Cost231LinkBudgetParameters {

    public enum UrbanEnvironment {
        MEDIUM_SIZE_CITY,
        DOWNTOWN
    }

    private double frequencyMHz = 915.0;

    private boolean los = true;

    private double streetWidthMeters = 20.0;

    private double buildingSeparationMeters = 50.0;

    // Parámetros físicos COST231 Walfisch-Ikegami (fase 6.2.3)
    private double baseStationHeightMeters = 30.0;

    private double averageBuildingHeightMeters = 15.0;

    private double mobileStationHeightMeters = 1.5;

    private double streetOrientationDegrees = 30.0;

    private UrbanEnvironment urbanEnvironment =
            UrbanEnvironment.MEDIUM_SIZE_CITY;

    /*
     * Parámetros legacy temporales.
     * Se conservan para que LinkBudgetService y NetworkServer actuales
     * sigan compilando hasta completar la fase 6.2.4.
     * No deben exponerse como parámetros editables en el frontend final.
     */
    private double hbMeters = 30.0;
    private double hrMeters = 1.5;
    private double loriDb = 0.0;
    private double kaDb = 54.0;
    private double kdDb = 18.0;
    private double kfDb = -4.0;
    private double lbshDb = 0.0;

    public void validate() {

        if (frequencyMHz <= 0.0) {
            throw new IllegalArgumentException(
                    "frequencyMHz debe ser mayor a 0");
        }

        if (streetWidthMeters <= 0.0) {
            throw new IllegalArgumentException(
                    "streetWidthMeters debe ser mayor a 0");
        }

        if (buildingSeparationMeters <= 0.0) {
            throw new IllegalArgumentException(
                    "buildingSeparationMeters debe ser mayor a 0");
        }

        if (baseStationHeightMeters <= 0.0) {
            throw new IllegalArgumentException(
                    "baseStationHeightMeters debe ser mayor a 0");
        }

        if (averageBuildingHeightMeters <= 0.0) {
            throw new IllegalArgumentException(
                    "averageBuildingHeightMeters debe ser mayor a 0");
        }

        if (mobileStationHeightMeters <= 0.0) {
            throw new IllegalArgumentException(
                    "mobileStationHeightMeters debe ser mayor a 0");
        }

        if (!los
                && averageBuildingHeightMeters
                <= mobileStationHeightMeters) {

            throw new IllegalArgumentException(
                    "En NLoS, averageBuildingHeightMeters debe ser mayor "
                    + "que mobileStationHeightMeters para calcular Lrts");
        }

        if (streetOrientationDegrees <= 0.0
                || streetOrientationDegrees >= 90.0) {

            throw new IllegalArgumentException(
                    "streetOrientationDegrees debe estar entre 0 y 90 grados");
        }

        if (urbanEnvironment == null) {
            throw new IllegalArgumentException(
                    "urbanEnvironment no puede ser null");
        }

        // Validación de compatibilidad temporal con la ruta legacy.
        if (hbMeters <= 0.0) {
            throw new IllegalArgumentException(
                    "hbMeters debe ser mayor a 0");
        }

        if (hrMeters <= 0.0) {
            throw new IllegalArgumentException(
                    "hrMeters debe ser mayor a 0");
        }
    }

    public double getFrequencyMHz() {
        return frequencyMHz;
    }

    public void setFrequencyMHz(double frequencyMHz) {
        this.frequencyMHz = frequencyMHz;
    }

    public boolean isLos() {
        return los;
    }

    public void setLos(boolean los) {
        this.los = los;
    }

    public double getStreetWidthMeters() {
        return streetWidthMeters;
    }

    public void setStreetWidthMeters(double streetWidthMeters) {
        this.streetWidthMeters = streetWidthMeters;
    }

    public double getBuildingSeparationMeters() {
        return buildingSeparationMeters;
    }

    public void setBuildingSeparationMeters(
            double buildingSeparationMeters) {

        this.buildingSeparationMeters =
                buildingSeparationMeters;
    }

    public double getBaseStationHeightMeters() {
        return baseStationHeightMeters;
    }

    public void setBaseStationHeightMeters(
            double baseStationHeightMeters) {

        this.baseStationHeightMeters =
                baseStationHeightMeters;
    }

    public double getAverageBuildingHeightMeters() {
        return averageBuildingHeightMeters;
    }

    public void setAverageBuildingHeightMeters(
            double averageBuildingHeightMeters) {

        this.averageBuildingHeightMeters =
                averageBuildingHeightMeters;
    }

    public double getMobileStationHeightMeters() {
        return mobileStationHeightMeters;
    }

    public void setMobileStationHeightMeters(
            double mobileStationHeightMeters) {

        this.mobileStationHeightMeters =
                mobileStationHeightMeters;
    }

    public double getStreetOrientationDegrees() {
        return streetOrientationDegrees;
    }

    public void setStreetOrientationDegrees(
            double streetOrientationDegrees) {

        this.streetOrientationDegrees =
                streetOrientationDegrees;
    }

    public UrbanEnvironment getUrbanEnvironment() {
        return urbanEnvironment;
    }

    public void setUrbanEnvironment(
            UrbanEnvironment urbanEnvironment) {

        this.urbanEnvironment =
                urbanEnvironment;
    }

    // -----------------------------------------------------------------
    // API legacy: retirar después de adaptar LinkBudgetService (6.2.4)
    // -----------------------------------------------------------------

    @Deprecated
    public double getHbMeters() {
        return hbMeters;
    }

    @Deprecated
    public void setHbMeters(double hbMeters) {
        this.hbMeters = hbMeters;
    }

    @Deprecated
    public double getHrMeters() {
        return hrMeters;
    }

    @Deprecated
    public void setHrMeters(double hrMeters) {
        this.hrMeters = hrMeters;
    }

    @Deprecated
    public double getLoriDb() {
        return loriDb;
    }

    @Deprecated
    public void setLoriDb(double loriDb) {
        this.loriDb = loriDb;
    }

    @Deprecated
    public double getKaDb() {
        return kaDb;
    }

    @Deprecated
    public void setKaDb(double kaDb) {
        this.kaDb = kaDb;
    }

    @Deprecated
    public double getKdDb() {
        return kdDb;
    }

    @Deprecated
    public void setKdDb(double kdDb) {
        this.kdDb = kdDb;
    }

    @Deprecated
    public double getKfDb() {
        return kfDb;
    }

    @Deprecated
    public void setKfDb(double kfDb) {
        this.kfDb = kfDb;
    }

    @Deprecated
    public double getLbshDb() {
        return lbshDb;
    }

    @Deprecated
    public void setLbshDb(double lbshDb) {
        this.lbshDb = lbshDb;
    }
}