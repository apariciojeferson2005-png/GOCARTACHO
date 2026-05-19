package com.gocartacho.gocartacho.repository;

import com.gocartacho.gocartacho.model.TokenRecuperacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository extends MongoRepository<TokenRecuperacion, String> {
    Optional<TokenRecuperacion> findByToken(String token);

    void deleteByEmailUsuario(String emailUsuario);
}