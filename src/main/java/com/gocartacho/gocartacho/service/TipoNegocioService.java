package com.gocartacho.gocartacho.service;

import com.gocartacho.gocartacho.model.TipoNegocio;
import java.util.List;
import java.util.Optional;

public interface TipoNegocioService {
    List<TipoNegocio> listarTodos();
    Optional<TipoNegocio> obtenerPorId(Long id);
    Optional<TipoNegocio> obtenerPorNombre(String nombre);
    TipoNegocio guardar(TipoNegocio tipoNegocio);
    void eliminar(Long id);
}
