package mx.mauricio.lorawan.performance;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class LinkBudgetServiceTest {

    @Test
    void marginShouldDecreaseWhenDistanceIncreases() {
        LinkBudgetService service = new LinkBudgetService();

        Cost231LinkBudgetParameters parameters =
        new Cost231LinkBudgetParameters();

        parameters.setFrequencyMHz(868.0);
        parameters.setLos(true);

        LinkBudgetResult r1 =
                service.evaluate(
                        "dev-1",
                        "gw-1",
                        100.0,
                        14.0,
                        -130.0,
                        parameters);

        LinkBudgetResult r2 =
                service.evaluate(
                        "dev-1",
                        "gw-1",
                        500.0,
                        14.0,
                        -130.0,
                        parameters);

        assertTrue(r2.getMarginDb() < r1.getMarginDb());
    }
}
