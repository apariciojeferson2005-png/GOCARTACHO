package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.Zona;
import com.gocartacho.gocartacho.repository.AfluenciaHistoricaRepository;
import com.gocartacho.gocartacho.repository.ZonaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("all")
public class ZonaServiceImplTest {

    @Mock
    private ZonaRepository zonaRepository;
    @Mock
    private AfluenciaHistoricaRepository afluenciaRepository;

    @InjectMocks
    private ZonaServiceImpl zonaService;

    @Test
    void obtenerZonaPorId_Exito() {
        Zona zona = new Zona();
        zona.setZonaId("zona-1");
        zona.setNombre("Centro");

        when(zonaRepository.findById("zona-1")).thenReturn(Optional.of(zona));

        Zona resultado = zonaService.obtenerZonaPorId("zona-1");

        assertNotNull(resultado);
        assertEquals("Centro", resultado.getNombre());
    }

    @Test
    void guardarZona_Exito() {
        Zona zona = new Zona();
        zona.setNombre("Nuevo Getsemaní");

        when(zonaRepository.save(any(Zona.class))).thenReturn(zona);

        Zona guardada = zonaService.guardarZona(zona);

        assertNotNull(guardada);
        assertEquals("Nuevo Getsemaní", guardada.getNombre());
        verify(zonaRepository, times(1)).save(zona);
    }
}
