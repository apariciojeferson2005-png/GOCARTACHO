package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.Favorito;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.repository.ComercioRepository;
import com.gocartacho.gocartacho.repository.FavoritoRepository;
import com.gocartacho.gocartacho.repository.UsuarioRepository;
import com.gocartacho.gocartacho.mapper.ComercioMapper;
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
public class FavoritoServiceImplTest {

    @Mock
    private FavoritoRepository favoritoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ComercioRepository comercioRepository;
    @Mock
    private ComercioMapper comercioMapper;

    @InjectMocks
    private FavoritoServiceImpl favoritoService;

    @Test
    void toggleFavorito_Agregar_Exito() {
        when(favoritoRepository.findByUsuarioIdAndComercioId("user-1", "com-1")).thenReturn(Optional.empty());
        
        Usuario usuario = new Usuario();
        usuario.setUsuarioId("user-1");
        when(usuarioRepository.findById("user-1")).thenReturn(Optional.of(usuario));
        
        Comercio comercio = new Comercio();
        comercio.setComercioId("com-1");
        when(comercioRepository.findById("com-1")).thenReturn(Optional.of(comercio));

        String resultado = favoritoService.toggleFavorito("user-1", "com-1");

        assertEquals("Comercio añadido a favoritos", resultado);
        verify(favoritoRepository, times(1)).save(any(Favorito.class));
    }

    @Test
    void toggleFavorito_Remover_Exito() {
        Favorito favorito = Favorito.builder().id("fav-1").build();
        when(favoritoRepository.findByUsuarioIdAndComercioId("user-1", "com-1")).thenReturn(Optional.of(favorito));

        String resultado = favoritoService.toggleFavorito("user-1", "com-1");

        assertEquals("Comercio removido de favoritos", resultado);
        verify(favoritoRepository, times(1)).deleteById("fav-1");
    }
}
