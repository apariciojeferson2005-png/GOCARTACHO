package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.Auditoria;
import com.gocartacho.gocartacho.repository.AuditoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("all")
public class AuditoriaServiceImplTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @InjectMocks
    private AuditoriaServiceImpl auditoriaService;

    @Test
    void registrarAccion_Exito() {
        auditoriaService.registrarAccion("admin@test.com", "LOGIN", "USUARIO", "1", "Login exitoso");

        verify(auditoriaRepository, times(1)).save(any(Auditoria.class));
    }
}
