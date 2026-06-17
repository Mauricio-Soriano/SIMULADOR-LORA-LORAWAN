package mx.mauricio.lorawan.adr;

public class AdrDecision {

    private final int newSf;

    public AdrDecision(int newSf) {
        this.newSf = newSf;
    }

    public int getNewSf() {
        return newSf;
    }
}
