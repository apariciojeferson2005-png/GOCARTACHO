package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.PlanComercio;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad PlanComercio en MongoDB.
 */
@Repository
public interface PlanComercioRepository extends MongoRepository<PlanComercio, String> {

    // Para la función: "Traer todos los comercios de ESTA plan"
    // Los ordenamos por el campo 'orden' para que aparezcan en la secuencia correcta.
    List<PlanComercio> findByPlanIdOrderByOrdenAsc(String planId);

    // Para encontrar todos los planes que contienen un comercio específico
    List<PlanComercio> findByComercioId(String comercioId);
}
