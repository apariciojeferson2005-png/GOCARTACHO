package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.AfluenciaHistorica;
import com.gocartacho.gocartacho.model.DiaSemana;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AfluenciaHistoricaRepository extends MongoRepository<AfluenciaHistorica, String> {

    Optional<AfluenciaHistorica> findByZonaIdAndDiaSemanaAndHora(String zonaId, DiaSemana diaSemana, Integer hora);

    /**
     * Búsqueda bulk para resolver el problema N+1 en ZonaServiceImpl.
     * Obtiene todos los registros de afluencia para un conjunto de zonas en una sola query a MongoDB.
     */
    List<AfluenciaHistorica> findByZonaIdInAndDiaSemanaAndHora(List<String> zonaIds, DiaSemana diaSemana, Integer hora);

    List<AfluenciaHistorica> findByZonaId(String zonaId);
}