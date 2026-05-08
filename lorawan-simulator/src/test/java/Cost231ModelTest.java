package mx.mauricio.lorawan.performance;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class Cost231ModelTest {

    @Test
    void losPathLossShouldIncreaseWithDistance() {
        Cost231WalfischIkegamiModel model = new Cost231WalfischIkegamiModel();
        double p1 = model.calculateLoSPathLossDb(100, 868);
        double p2 = model.calculateLoSPathLossDb(500, 868);
        assertTrue(p2 > p1);
    }

    @Test
    void nlosPathLossShouldBePositive() {
        Cost231WalfischIkegamiModel model = new Cost231WalfischIkegamiModel();
        double p = model.calculateNLoSPathLossDb(1000, 868, 20, 30, 1.5, 0, 50, 0, 0, 0, 0);
        assertTrue(p > 0);
    }
}
