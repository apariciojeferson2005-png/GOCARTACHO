package com.gocartacho.gocartacho.service;

import com.gocartacho.gocartacho.model.Promocion;
import com.gocartacho.gocartacho.dto.PromocionDTO;
import java.util.List;

/**
 * Interfaz que define las operaciones de negocio para Promocion.
 */
public interface PromocionService {

    List<Promocion> obtenerPromocionesActivas();

    List<Promocion> obtenerPromocionesActivasPorZona(String zonaId);

    List<Promocion> obtenerPromocionesActivasPorComercio(String comercioId);

    Promocion crearPromocion(Promocion promocion, String comercioId);

    void desactivarPromocion(String promocionId);

    PromocionDTO buscarPorId(String id);

    Promocion buscarEntidadPorId(String id);

    void cambiarEstado(String id, boolean activa);
}

