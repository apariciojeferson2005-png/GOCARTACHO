package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.PuntoCalor;
import com.gocartacho.gocartacho.repository.AfluenciaHistoricaRepository;
import com.gocartacho.gocartacho.repository.PuntoCalorRepository;
import com.gocartacho.gocartacho.repository.ZonaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("all")
public class MapaCalorServiceImplTest {

    @Mock
    private PuntoCalorRepository puntoCalorRepository;
    @Mock
    private AfluenciaHistoricaRepository afluenciaHistoricaRepository;
    @Mock
    private ZonaRepository zonaRepository;

    @InjectMocks
    private MapaCalorServiceImpl mapaCalorService;

    @Test
    void guardarPuntoCalor_Nuevo_Exito() {
        PuntoCalor punto = new PuntoCalor();
        punto.setDispositivoHash("hash-123");
        punto.setLatitud(new BigDecimal("4.5"));
        punto.setLongitud(new BigDecimal("-74.0"));

        when(puntoCalorRepository.findByDispositivoHash("hash-123")).thenReturn(Optional.empty());

        mapaCalorService.guardarPuntoCalor(punto);

        verify(puntoCalorRepository, times(1)).save(any(PuntoCalor.class));
    }

    @Test
    void guardarPuntoCalor_Actualizar_Exito() {
        PuntoCalor existente = new PuntoCalor();
        existente.setDispositivoHash("hash-123");
        
        PuntoCalor nuevo = new PuntoCalor();
        nuevo.setDispositivoHash("hash-123");
        nuevo.setLatitud(new BigDecimal("4.6"));
        nuevo.setLongitud(new BigDecimal("-74.1"));

        when(puntoCalorRepository.findByDispositivoHash("hash-123")).thenReturn(Optional.of(existente));

        mapaCalorService.guardarPuntoCalor(nuevo);

        verify(puntoCalorRepository, times(1)).save(existente);
    }
}
