package mx.mauricio.lorawan.performance;

public class LinkBudgetService {

    private final Cost231WalfischIkegamiModel model;

    public LinkBudgetService() {
        this(new Cost231WalfischIkegamiModel());
    }

    public LinkBudgetService(
            Cost231WalfischIkegamiModel model) {

        if (model == null) {
            throw new IllegalArgumentException(
                    "Cost231WalfischIkegamiModel no puede ser null");
        }

        this.model = model;
    }

    public LinkBudgetResult evaluate(
            String deviceId,
            String gatewayId,
            double distanceMeters,
            double txPowerDbm,
            double sensitivityDbm,
            Cost231LinkBudgetParameters parameters) {

        if (parameters == null) {
            parameters = new Cost231LinkBudgetParameters();
        }

        parameters.validate();

        if (distanceMeters <= 0.0) {
            throw new IllegalArgumentException(
                    "distanceMeters debe ser mayor a 0");
        }

        double pathLoss =
                model.calculatePathLossDb(
                        distanceMeters,
                        parameters);

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
                parameters.isLos());
    }
}
