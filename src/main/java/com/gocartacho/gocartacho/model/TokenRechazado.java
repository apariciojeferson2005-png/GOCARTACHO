package com.gocartacho.gocartacho.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "tokens_rechazados")
public class TokenRechazado {

    @Id
    private String id;

    @Indexed(unique = true)
    private String token;

    // MongoDB eliminará automáticamente el documento cuando la fecha actual
    // alcance o supere este valor gracias a expireAfterSeconds = 0
    @Indexed(expireAfterSeconds = 0)
    private Date fechaExpiracion;
}