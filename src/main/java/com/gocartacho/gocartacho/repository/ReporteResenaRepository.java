package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.ReporteResena;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReporteResenaRepository extends MongoRepository<ReporteResena, String> {
    List<ReporteResena> findByResenaId(String resenaId);
    void deleteByResenaId(String resenaId);
    long countByResenaId(String resenaId);
}
