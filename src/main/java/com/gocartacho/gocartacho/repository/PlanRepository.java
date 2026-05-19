package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.Plan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de acceso a datos para la entidad Plan en MongoDB.
 */
@Repository
public interface PlanRepository extends MongoRepository<Plan, String> {

}
