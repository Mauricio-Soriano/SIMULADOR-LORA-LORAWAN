package mx.mauricio.lorawan.source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class FuenteInformacion implements IFuenteVisualizable {

    private static final String[] FORMATOS_PERMITIDOS = {"txt", "csv"};

    private final Path rutaArchivo;
    private List<String> lineas = Collections.emptyList();

    public FuenteInformacion(String rutaArchivo) {
        this.rutaArchivo = resolverRuta(rutaArchivo);
        validarFormato();
    }

    private Path resolverRuta(String rutaOriginal) {
        Path directa = Path.of(rutaOriginal);
        if (Files.exists(directa)) {
            return directa;
        }

        Path enData = Path.of("data", directa.getFileName().toString());
        if (Files.exists(enData)) {
            return enData;
        }

        return directa;
    }

    private void validarFormato() {
        String nombre = rutaArchivo.getFileName().toString();
        int idx = nombre.lastIndexOf('.');
        String extension = (idx >= 0) ? nombre.substring(idx + 1).toLowerCase() : "";
        boolean permitido = false;

        for (String f : FORMATOS_PERMITIDOS) {
            if (f.equals(extension)) {
                permitido = true;
                break;
            }
        }

        if (!permitido) {
            throw new IllegalArgumentException("Formato no permitido: " + extension);
        }
    }

    @Override
    public void cargarInformacion() throws IOException {
        this.lineas = Files.readAllLines(rutaArchivo);
        imprimirPreview();
    }

    @Override
    public void eliminarDatos() {
        this.lineas = Collections.emptyList();
    }

    @Override
    public void volverACargar() throws IOException {
        eliminarDatos();
        cargarInformacion();
    }

    public List<String> getLineas() {
        return lineas;
    }

    private void imprimirPreview() {
        System.out.println("== Preview de " + rutaArchivo + " ==");
        int limite = Math.min(IFuenteVisualizable.MAX_ELEMENTOS, lineas.size());
        for (int i = 0; i < limite; i++) {
            System.out.println((i + 1) + ": " + lineas.get(i));
        }
        if (lineas.size() > IFuenteVisualizable.MAX_ELEMENTOS) {
            System.out.println("... y " + (lineas.size() - IFuenteVisualizable.MAX_ELEMENTOS) + " líneas más");
        }
    }
}


