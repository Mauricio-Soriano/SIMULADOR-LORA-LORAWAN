package mx.mauricio.lorawan.performance;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class LinkBudgetServiceTest {

    @Test
    void marginShouldDecreaseWhenDistanceIncreases() {
        LinkBudgetService service = new LinkBudgetService();

        LinkBudgetResult r1 = service.evaluate(
                "dev-1", "gw-1", 100, 868, 14, -130,
                true, 20, 30, 1.5, 0, 50, 0, 0, 0, 0
        );

        LinkBudgetResult r2 = service.evaluate(
                "dev-1", "gw-1", 500, 868, 14, -130,
                true, 20, 30, 1.5, 0, 50, 0, 0, 0, 0
        );

        assertTrue(r2.getMarginDb() < r1.getMarginDb());
    }
}
