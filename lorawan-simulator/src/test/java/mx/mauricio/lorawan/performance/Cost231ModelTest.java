package mx.mauricio.lorawan.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import mx.mauricio.lorawan.performance.Cost231LinkBudgetParameters.UrbanEnvironment;

public class Cost231ModelTest {

    @Test
    void losPathLossShouldIncreaseWithDistance() {

        Cost231WalfischIkegamiModel model =
                new Cost231WalfischIkegamiModel();

        double loss100m =
                model.calculateLoSPathLossDb(
                        100.0,
                        900.0);

        double loss500m =
                model.calculateLoSPathLossDb(
                        500.0,
                        900.0);

        assertTrue(
                loss500m > loss100m,
                "La pérdida LoS debe aumentar al aumentar la distancia");
    }

    @Test
    void documentedNlosScenarioShouldReproduceCalculatedValues() {

        Cost231WalfischIkegamiModel model =
                new Cost231WalfischIkegamiModel();

        Cost231LinkBudgetParameters parameters =
                buildDocumentedScenario();

        /*
         * Escenario documental COST231:
         * f = 900 MHz
         * hBS = 30 m
         * hBuildings = 15 m
         * hMobile = 3 m
         * street orientation = 30 degrees
         * street width = 25 m
         * building separation = 50 m
         * environment = MEDIUM_SIZE_CITY
         *
         * Valores obtenidos al evaluar las ecuaciones implementadas:
         * 1 km -> 117.4437 dB
         * 2 km -> 128.8829 dB
         * 3 km -> 135.5744 dB
         */

        assertEquals(
                117.4437,
                model.calculatePathLossDb(
                        1000.0,
                        parameters),
                0.01);

        assertEquals(
                128.8829,
                model.calculatePathLossDb(
                        2000.0,
                        parameters),
                0.01);

        assertEquals(
                135.5744,
                model.calculatePathLossDb(
                        3000.0,
                        parameters),
                0.01);
    }

    @Test
    void documentedNlosScenarioShouldRemainWithinOneDbOfPublishedTable() {

        Cost231WalfischIkegamiModel model =
                new Cost231WalfischIkegamiModel();

        Cost231LinkBudgetParameters parameters =
                buildDocumentedScenario();

        /*
         * La tabla del caso publicado presenta los resultados como enteros:
         * 1 km -> 117 dB
         * 2 km -> 128 dB
         * 3 km -> 135 dB
         *
         * Se utiliza una tolerancia de 1 dB porque la publicación no
         * presenta los valores intermedios ni el criterio exacto de
         * redondeo/truncamiento utilizado para la tabla.
         */

        assertEquals(
                117.0,
                model.calculatePathLossDb(
                        1000.0,
                        parameters),
                1.0);

        assertEquals(
                128.0,
                model.calculatePathLossDb(
                        2000.0,
                        parameters),
                1.0);

        assertEquals(
                135.0,
                model.calculatePathLossDb(
                        3000.0,
                        parameters),
                1.0);
    }

    private Cost231LinkBudgetParameters buildDocumentedScenario() {

        Cost231LinkBudgetParameters parameters =
                new Cost231LinkBudgetParameters();

        parameters.setFrequencyMHz(900.0);
        parameters.setLos(false);
        parameters.setStreetWidthMeters(25.0);
        parameters.setBaseStationHeightMeters(30.0);
        parameters.setAverageBuildingHeightMeters(15.0);
        parameters.setMobileStationHeightMeters(3.0);
        parameters.setStreetOrientationDegrees(30.0);
        parameters.setBuildingSeparationMeters(50.0);
        parameters.setUrbanEnvironment(
                UrbanEnvironment.MEDIUM_SIZE_CITY);

        return parameters;
    }
}