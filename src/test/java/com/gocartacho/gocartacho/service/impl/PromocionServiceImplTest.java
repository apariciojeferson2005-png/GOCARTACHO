package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.Promocion;
import com.gocartacho.gocartacho.repository.ComercioRepository;
import com.gocartacho.gocartacho.repository.PromocionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("all")
public class PromocionServiceImplTest {

    @Mock
    private PromocionRepository promocionRepository;
    @Mock
    private ComercioRepository comercioRepository;

    @InjectMocks
    private PromocionServiceImpl promocionService;

    @Test
    void crearPromocion_Exito() {
        Comercio comercio = new Comercio();
        comercio.setComercioId("com-789");

        Promocion promo = new Promocion();
        promo.setTitulo("Promo 2x1");

        when(comercioRepository.findById("com-789")).thenReturn(Optional.of(comercio));
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(i -> i.getArguments()[0]);

        Promocion resultado = promocionService.crearPromocion(promo, "com-789");

        assertNotNull(resultado);
        assertEquals("com-789", resultado.getComercioId());
        assertTrue(resultado.getActiva());
        verify(promocionRepository, times(1)).save(promo);
    }

    @Test
    void crearPromocion_ComercioNoEncontrado_LanzaExcepcion() {
        when(comercioRepository.findById("invalid")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> promocionService.crearPromocion(new Promocion(), "invalid"));
    }

    @Test
    void desactivarPromocion_Exito() {
        Promocion promo = new Promocion();
        promo.setPromocionId("promo-1");
        promo.setActiva(true);

        when(promocionRepository.findById("promo-1")).thenReturn(Optional.of(promo));

        promocionService.desactivarPromocion("promo-1");

        assertFalse(promo.getActiva());
        verify(promocionRepository, times(1)).save(promo);
    }
}
