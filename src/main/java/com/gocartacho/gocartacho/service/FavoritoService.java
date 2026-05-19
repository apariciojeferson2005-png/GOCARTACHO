package com.gocartacho.gocartacho.service;

import com.gocartacho.gocartacho.dto.ComercioDTO;

import java.util.List;

/**
 * Interfaz que define las operaciones de negocio para Favorito.
 */
public interface FavoritoService {
    String toggleFavorito(String usuarioId, String comercioId);
    List<ComercioDTO> obtenerFavoritosUsuario(String usuarioId);
}

