package com.gocartacho.gocartacho.service;

import com.gocartacho.gocartacho.model.Zona;
import java.util.List;

/**
 * Interfaz que define las operaciones de negocio para Zona.
 */
public interface ZonaService {
    
    List<Zona> obtenerTodasLasZonas();
    
    Zona obtenerZonaPorId(String id);

    /** Permite buscar la zona por su número entero (1, 2, 3...) en lugar del ObjectId. */
    Zona obtenerZonaPorNumero(Integer numero);
    
    Zona guardarZona(Zona zona);
    
    void eliminarZona(String id);
}
