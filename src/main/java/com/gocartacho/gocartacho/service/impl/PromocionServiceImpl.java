package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.dto.PromocionDTO;
import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.EstadoComercio;
import com.gocartacho.gocartacho.model.Promocion;
import com.gocartacho.gocartacho.repository.ComercioRepository;
import com.gocartacho.gocartacho.repository.PromocionRepository;
import com.gocartacho.gocartacho.service.PromocionService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de la lógica de negocio para Promocion.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PromocionServiceImpl implements PromocionService {

    private final PromocionRepository promocionRepository;
    private final ComercioRepository comercioRepository;

    @Override
    public List<Promocion> obtenerPromocionesActivas() {
        return promocionRepository.findPromocionesActivas(LocalDate.now());
    }

    @Override
    public List<Promocion> obtenerPromocionesActivasPorZona(String zonaId) {
        if (zonaId == null) return List.of();
        
        List<Comercio> comercios = comercioRepository.findByZonaIdAndEstadoAprobacion(zonaId, EstadoComercio.APROBADO);
        if (comercios.isEmpty()) {
            return List.of();
        }
        List<String> comercioIds = comercios.stream().map(Comercio::getComercioId).collect(Collectors.toList());
        return promocionRepository.findPromocionesActivasPorComercios(LocalDate.now(), comercioIds);
    }

    @Override
    public List<Promocion> obtenerPromocionesActivasPorComercio(String comercioId) {
        if (comercioId == null) return List.of();
        return promocionRepository.findPromocionesActivasPorComercio(LocalDate.now(), comercioId);
    }

    @Override
    @Transactional
    public Promocion crearPromocion(Promocion promocion, String comercioId) {
        if (promocion == null || comercioId == null) {
            throw new IllegalArgumentException("Datos de promoción inválidos.");
        }
        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> new IllegalArgumentException("Comercio no encontrado."));
        
        promocion.setComercioId(comercio.getComercioId());
        promocion.setActiva(true);
        return promocionRepository.save(promocion);
    }

    @Override
    @Transactional
    public void desactivarPromocion(String promocionId) {
        Objects.requireNonNull(promocionId, "ID de promoción es obligatorio");
        Promocion promo = promocionRepository.findById(promocionId)
                .orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada."));
        promo.setActiva(false);
        promocionRepository.save(promo);
    }

    @Override
    public PromocionDTO buscarPorId(String id) {
        Promocion promocion = buscarEntidadPorId(id);
        return toDto(promocion);
    }

    @Override
    public Promocion buscarEntidadPorId(String id) {
        Objects.requireNonNull(id, "ID de promoción es obligatorio");
        return promocionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Promoción no encontrada"));
    }

    private PromocionDTO toDto(Promocion promocion) {
        PromocionDTO dto = new PromocionDTO();
        dto.setId(promocion.getPromocionId());
        dto.setTitulo(promocion.getTitulo());
        dto.setDescripcion(promocion.getDescripcion());
        dto.setActiva(Boolean.TRUE.equals(promocion.getActiva()));
        return dto;
    }

    @Override
    @Transactional
    public void cambiarEstado(String id, boolean activa) {
        java.util.Objects.requireNonNull(id, "ID de promoción es obligatorio");
        Promocion promo = promocionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada."));
        promo.setActiva(activa);
        promocionRepository.save(promo);
    }
}

