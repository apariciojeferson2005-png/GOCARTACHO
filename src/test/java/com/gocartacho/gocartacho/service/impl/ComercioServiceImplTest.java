package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.EstadoComercio;
import com.gocartacho.gocartacho.repository.ComercioRepository;
import com.gocartacho.gocartacho.repository.ZonaRepository;
import com.gocartacho.gocartacho.service.TipoNegocioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("all")
public class ComercioServiceImplTest {

    @Mock
    private ComercioRepository comercioRepository;
    @Mock
    private ZonaRepository zonaRepository;
    @Mock
    private TipoNegocioService tipoNegocioService;

    @InjectMocks
    private ComercioServiceImpl comercioService;

    private Comercio comercioTest;

    @BeforeEach
    void setUp() {
        comercioTest = Comercio.builder()
                .comercioId("com-123")
                .nombre("Negocio de Prueba")
                .estadoAprobacion(EstadoComercio.PENDIENTE)
                .latitud(new BigDecimal("4.5"))
                .longitud(new BigDecimal("-74.0"))
                .tipoNegocioId(1L)
                .build();
    }

    @Test
    void obtenerComercioPorId_Exito() {
        when(comercioRepository.findById("com-123")).thenReturn(Optional.of(comercioTest));
        when(tipoNegocioService.obtenerPorId(1L)).thenReturn(Optional.empty());

        Comercio resultado = comercioService.obtenerComercioPorId("com-123");

        assertNotNull(resultado);
        assertEquals("Negocio de Prueba", resultado.getNombre());
        verify(comercioRepository, times(1)).findById("com-123");
    }

    @Test
    void guardarComercio_Exito() {
        when(comercioRepository.save(any(Comercio.class))).thenReturn(comercioTest);

        Comercio resultado = comercioService.guardarComercio(comercioTest);

        assertNotNull(resultado);
        assertNotNull(resultado.getUbicacion()); // Debe haberse sincronizado el GeoJsonPoint
        assertEquals(EstadoComercio.PENDIENTE, resultado.getEstadoAprobacion());
        verify(comercioRepository, times(1)).save(any(Comercio.class));
    }

    @Test
    void guardarComercio_ObjetoNulo_LanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> comercioService.guardarComercio(null));
    }

    @Test
    void eliminarComercio_Exito() {
        doNothing().when(comercioRepository).deleteById("com-123");

        comercioService.eliminarComercio("com-123");

        verify(comercioRepository, times(1)).deleteById("com-123");
    }
}
