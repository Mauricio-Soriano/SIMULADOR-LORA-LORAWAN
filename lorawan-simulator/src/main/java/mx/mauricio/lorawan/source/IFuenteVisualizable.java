package mx.mauricio.lorawan.source;

import java.io.IOException;

public interface IFuenteVisualizable {

    int MAX_ELEMENTOS = 10;

    void cargarInformacion() throws IOException;

    void eliminarDatos();

    void volverACargar() throws IOException;
}

