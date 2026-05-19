package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.model.RolUsuario;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("all")
public class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Test
    void registrarUsuario_EmailDuplicado_LanzaExcepcion() {
        Usuario usuario = new Usuario();
        usuario.setEmail("test@gocartacho.com");
        usuario.setContrasena("12345");
        usuario.setUsername("testuser");

        when(usuarioRepository.findByEmail("test@gocartacho.com")).thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void registrarUsuario_Exito() {
        Usuario usuario = new Usuario();
        usuario.setEmail("nuevo@gocartacho.com");
        usuario.setContrasena("hunter2");
        usuario.setNombre("Test");
        usuario.setApellido("User");
        usuario.setUsername("testuser");

        when(usuarioRepository.findByEmail("nuevo@gocartacho.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("hunter2")).thenReturn("hash123");
        
        Usuario guardado = new Usuario();
        guardado.setEmail("nuevo@gocartacho.com");
        guardado.setRol(RolUsuario.USER);
        guardado.setUsername("testuser");
        
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(guardado);

        Usuario r = usuarioService.registrarUsuario(usuario);
        assertEquals(RolUsuario.USER, r.getRol());
        assertEquals("nuevo@gocartacho.com", r.getEmail());
        assertEquals("testuser", r.getUsername());
    }
}
