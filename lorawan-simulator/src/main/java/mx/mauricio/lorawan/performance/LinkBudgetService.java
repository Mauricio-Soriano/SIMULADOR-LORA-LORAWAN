package mx.mauricio.lorawan.performance;

public class LinkBudgetService {

    private final Cost231WalfischIkegamiModel model;

    public LinkBudgetService() {

        this(new Cost231WalfischIkegamiModel());
    }

    public LinkBudgetService(
            Cost231WalfischIkegamiModel model) {

        this.model =
                model;
    }

    public LinkBudgetResult evaluate(
            String deviceId,
            String gatewayId,
            double distanceMeters,
            double txPowerDbm,
            double sensitivityDbm,
            Cost231LinkBudgetParameters parameters) {

        if (parameters == null) {

            parameters =
                    new Cost231LinkBudgetParameters();
        }

        parameters.validate();

        return evaluate(
                deviceId,
                gatewayId,
                distanceMeters,
                parameters.getFrequencyMHz(),
                txPowerDbm,
                sensitivityDbm,
                parameters.isLos(),
                parameters.getStreetWidthMeters(),
                parameters.getHbMeters(),
                parameters.getHrMeters(),
                parameters.getLoriDb(),
                parameters.getBuildingSeparationMeters(),
                parameters.getKaDb(),
                parameters.getKdDb(),
                parameters.getKfDb(),
                parameters.getLbshDb());
    }

    public LinkBudgetResult evaluate(
            String deviceId,
            String gatewayId,
            double distanceMeters,
            double frequencyMHz,
            double txPowerDbm,
            double sensitivityDbm,
            boolean los,
            double streetWidthMeters,
            double hbMeters,
            double hrMeters,
            double loriDb,
            double buildingSeparationMeters,
            double kaDb,
            double kdDb,
            double kfDb,
            double lbshDb) {

        double pathLoss =
                model.calculatePathLossDb(
                        los,
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

        double rxPower =
                txPowerDbm - pathLoss;

        double margin =
                rxPower - sensitivityDbm;

        return new LinkBudgetResult(
                deviceId,
                gatewayId,
                distanceMeters,
                pathLoss,
                txPowerDbm,
                rxPower,
                sensitivityDbm,
                margin,
                los);
    }
}
