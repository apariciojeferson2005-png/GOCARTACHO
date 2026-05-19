package com.gocartacho.gocartacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "tokens_recuperacion")
public class TokenRecuperacion {

    @Id
    private String id;
    private String token;

    @Indexed
    private String emailUsuario;
    private LocalDateTime fechaExpiracion;

}