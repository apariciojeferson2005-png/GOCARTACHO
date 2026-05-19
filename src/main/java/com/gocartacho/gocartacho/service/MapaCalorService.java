package com.gocartacho.gocartacho.service;

import com.gocartacho.gocartacho.dto.PuntoMapaCalorDTO;
import com.gocartacho.gocartacho.model.DiaSemana;
import com.gocartacho.gocartacho.model.NivelAfluencia;
import com.gocartacho.gocartacho.model.PuntoCalor;
import com.gocartacho.gocartacho.model.AfluenciaHistorica;
import java.util.List;

public interface MapaCalorService {

    /**
     * Guarda un nuevo ping de ubicación anónimo.
     */
    void guardarPuntoCalor(PuntoCalor puntoCalor);

    /**
     * Obtiene los pings recientes para el mapa de calor en tiempo real.
     * (Ej. últimos 15 minutos)
     */
    List<PuntoMapaCalorDTO> obtenerPuntosCalorTiempoReal();

    /**
     * Obtiene el nivel de afluencia promedio para el filtro histórico.
     */
    NivelAfluencia obtenerAfluenciaHistorica(String zonaId, DiaSemana dia, int hora);

    /**
     * Obtiene todo el historial de afluencia para una zona específica.
     */
    List<AfluenciaHistorica> obtenerHistorialPorZona(String zonaId);
}