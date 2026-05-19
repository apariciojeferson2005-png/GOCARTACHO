package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.TipoNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoNegocioRepository extends JpaRepository<TipoNegocio, Long> {
    Optional<TipoNegocio> findByNombreIgnoreCase(String nombre);
}
