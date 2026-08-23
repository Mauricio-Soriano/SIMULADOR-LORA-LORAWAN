package mx.mauricio.lorawan.performance;

public class Cost231LinkBudgetParameters {

    private double frequencyMHz = 915.0;

    private boolean los = true;

    private double streetWidthMeters = 20.0;

    private double hbMeters = 30.0;

    private double hrMeters = 1.5;

    private double loriDb = 0.0;

    private double buildingSeparationMeters = 50.0;

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

        if (hbMeters <= 0.0) {
            throw new IllegalArgumentException(
                    "hbMeters debe ser mayor a 0");
        }

        if (hrMeters <= 0.0) {
            throw new IllegalArgumentException(
                    "hrMeters debe ser mayor a 0");
        }

        if (buildingSeparationMeters <= 0.0) {
            throw new IllegalArgumentException(
                    "buildingSeparationMeters debe ser mayor a 0");
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

    public double getHbMeters() {
        return hbMeters;
    }

    public void setHbMeters(double hbMeters) {
        this.hbMeters = hbMeters;
    }

    public double getHrMeters() {
        return hrMeters;
    }

    public void setHrMeters(double hrMeters) {
        this.hrMeters = hrMeters;
    }

    public double getLoriDb() {
        return loriDb;
    }

    public void setLoriDb(double loriDb) {
        this.loriDb = loriDb;
    }

    public double getBuildingSeparationMeters() {
        return buildingSeparationMeters;
    }

    public void setBuildingSeparationMeters(
            double buildingSeparationMeters) {

        this.buildingSeparationMeters =
                buildingSeparationMeters;
    }

    public double getKaDb() {
        return kaDb;
    }

    public void setKaDb(double kaDb) {
        this.kaDb = kaDb;
    }

    public double getKdDb() {
        return kdDb;
    }

    public void setKdDb(double kdDb) {
        this.kdDb = kdDb;
    }

    public double getKfDb() {
        return kfDb;
    }

    public void setKfDb(double kfDb) {
        this.kfDb = kfDb;
    }

    public double getLbshDb() {
        return lbshDb;
    }

    public void setLbshDb(double lbshDb) {
        this.lbshDb = lbshDb;
    }
}