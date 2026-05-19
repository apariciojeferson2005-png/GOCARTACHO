package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.dto.ResenaRequest;
import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.Resena;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.repository.ComercioRepository;
import com.gocartacho.gocartacho.repository.ResenaRepository;
import com.gocartacho.gocartacho.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
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
public class ResenaServiceImplTest {

    @Mock
    private ResenaRepository resenaRepository;
    @Mock
    private ComercioRepository comercioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ResenaServiceImpl resenaService;

    private Usuario usuario;
    private Comercio comercio;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setUsuarioId("1");
        usuario.setEmail("test@gocartacho.com");

        comercio = new Comercio();
        comercio.setComercioId("10");
        comercio.setNombre("Bar Test");
    }

    @Test
    void guardarResena_Exito() {
        ResenaRequest req = new ResenaRequest();
        req.setComercioId("10");
        req.setCalificacion(5);
        req.setComentario("Excelente");

        when(usuarioRepository.findById("1")).thenReturn(Optional.of(usuario));
        when(comercioRepository.findById("10")).thenReturn(Optional.of(comercio));

        Resena mockResena = new Resena();
        mockResena.setResenaId("123");
        when(resenaRepository.save(java.util.Objects.requireNonNull(any(Resena.class)))).thenReturn(java.util.Objects.requireNonNull(mockResena));

        Resena resultado = resenaService.guardarResena(req, "1");
        
        assertEquals("123", resultado.getResenaId());
        verify(resenaRepository).save(java.util.Objects.requireNonNull(any(Resena.class)));
    }

    @Test
    void guardarResena_UsuarioYaComento_LanzaExcepcion() {
        ResenaRequest req = new ResenaRequest();
        req.setComercioId("10");
        req.setCalificacion(5);
        req.setComentario("Excelente");

        when(usuarioRepository.findById("1")).thenReturn(Optional.of(usuario));
        when(comercioRepository.findById("10")).thenReturn(Optional.of(comercio));
        // Simulamos que el usuario ya había comentado en este comercio
        when(resenaRepository.existsByUsuarioIdAndComercioId("1", "10")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> resenaService.guardarResena(req, "1"));
        verify(resenaRepository, never()).save(any(Resena.class));
    }

    @Test
    void eliminarResena_Exito() {
        Resena resena = new Resena();
        resena.setResenaId("123");
        resena.setUsuarioId("1");

        when(resenaRepository.findById("123")).thenReturn(Optional.of(resena));
        
        assertDoesNotThrow(() -> resenaService.eliminarResena("123", "1"));
        verify(resenaRepository).delete(resena);
    }
    
    @Test
    void eliminarResena_NoAutorizado() {
        Resena resena = new Resena();
        resena.setResenaId("123");
        resena.setUsuarioId("2"); // Otro autor

        when(resenaRepository.findById("123")).thenReturn(Optional.of(resena));
        
        assertThrows(IllegalArgumentException.class, () -> resenaService.eliminarResena("123", "1"));
        verify(resenaRepository, never()).delete(java.util.Objects.requireNonNull(any()));
    }
}
