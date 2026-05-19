package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.TipoNegocio;
import com.gocartacho.gocartacho.repository.TipoNegocioRepository;
import com.gocartacho.gocartacho.service.TipoNegocioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TipoNegocioServiceImpl implements TipoNegocioService {

    private final TipoNegocioRepository tipoNegocioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TipoNegocio> listarTodos() {
        return tipoNegocioRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TipoNegocio> obtenerPorId(Long id) {
        if (id == null) return Optional.empty();
        return tipoNegocioRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TipoNegocio> obtenerPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return Optional.empty();
        return tipoNegocioRepository.findByNombreIgnoreCase(nombre);
    }

    @Override
    @Transactional
    public TipoNegocio guardar(TipoNegocio tipoNegocio) {
        if (tipoNegocio == null) {
            throw new IllegalArgumentException("El tipo de negocio no puede ser nulo");
        }
        return tipoNegocioRepository.save(tipoNegocio);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (id != null) {
            tipoNegocioRepository.deleteById(id);
        }
    }
}
