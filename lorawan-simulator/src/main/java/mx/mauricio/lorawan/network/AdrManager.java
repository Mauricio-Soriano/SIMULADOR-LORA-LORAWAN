package mx.mauricio.lorawan.network;

/**
 * Política ADR simplificada del simulador.
 *
 * IMPORTANTE:
 * - LoRaWAN permite que la red administre el data rate y la potencia RF mediante ADR.
 * - Los umbrales de margen usados aquí NO son valores normativos definidos por LoRaWAN.
 * - Se emplea una banda de histéresis para evitar oscilaciones entre SF consecutivos.
 *
 * Criterio interno del simulador:
 * - margen < 3 dB  -> aumentar SF un nivel (mayor robustez)
 * - 3..6 dB       -> mantener SF
 * - margen > 6 dB -> disminuir SF un nivel (mayor eficiencia)
 *
 * Los límites 3/6 dB se documentan como parámetros de diseño del simulador.
 */
public class AdrManager {

    public static final int MIN_SF = 7;
    public static final int MAX_SF = 12;

    public static final double LOW_MARGIN_THRESHOLD_DB = 3.0;
    public static final double HIGH_MARGIN_THRESHOLD_DB = 6.0;

    public int recommendSpreadingFactor(
            double linkMarginDb,
            int currentSf) {

        validateCurrentSf(currentSf);

        if (!Double.isFinite(linkMarginDb)) {
            throw new IllegalArgumentException(
                    "El margen de enlace debe ser un valor finito.");
        }

        if (linkMarginDb < LOW_MARGIN_THRESHOLD_DB) {
            return Math.min(
                    currentSf + 1,
                    MAX_SF);
        }

        if (linkMarginDb > HIGH_MARGIN_THRESHOLD_DB) {
            return Math.max(
                    currentSf - 1,
                    MIN_SF);
        }

        return currentSf;
    }

    private void validateCurrentSf(int currentSf) {

        if (currentSf < MIN_SF
                || currentSf > MAX_SF) {

            throw new IllegalArgumentException(
                    "El SF actual debe estar entre "
                    + MIN_SF
                    + " y "
                    + MAX_SF
                    + ".");
        }
    }
}
