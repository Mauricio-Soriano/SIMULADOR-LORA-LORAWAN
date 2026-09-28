package mx.mauricio.lorawan.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class AdrManagerTest {

    private final AdrManager adrManager =
            new AdrManager();

    @Test
    void lowMarginShouldIncreaseSfOneStep() {

        assertEquals(
                9,
                adrManager.recommendSpreadingFactor(
                        2.0,
                        8));
    }

    @Test
    void stableMarginShouldKeepCurrentSf() {

        assertEquals(
                8,
                adrManager.recommendSpreadingFactor(
                        4.5,
                        8));
    }

    @Test
    void highMarginShouldDecreaseSfOneStep() {

        assertEquals(
                7,
                adrManager.recommendSpreadingFactor(
                        10.0,
                        8));
    }

    @Test
    void sfShouldNeverGoBelowSeven() {

        assertEquals(
                7,
                adrManager.recommendSpreadingFactor(
                        17.43,
                        7));
    }

    @Test
    void sfShouldNeverGoAboveTwelve() {

        assertEquals(
                12,
                adrManager.recommendSpreadingFactor(
                        -2.0,
                        12));
    }

    @Test
    void exactHysteresisBoundariesShouldKeepSf() {

        assertEquals(
                9,
                adrManager.recommendSpreadingFactor(
                        3.0,
                        9));

        assertEquals(
                9,
                adrManager.recommendSpreadingFactor(
                        6.0,
                        9));
    }

    @Test
    void invalidSfShouldBeRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> adrManager
                        .recommendSpreadingFactor(
                                5.0,
                                6));
    }
}