package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.dto.ComercioDTO;
import com.gocartacho.gocartacho.mapper.ComercioMapper;
import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.Favorito;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.repository.ComercioRepository;
import com.gocartacho.gocartacho.repository.FavoritoRepository;
import com.gocartacho.gocartacho.repository.UsuarioRepository;
import com.gocartacho.gocartacho.service.FavoritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación de la lógica de negocio para Favorito.
 */
@Service
@RequiredArgsConstructor
public class FavoritoServiceImpl implements FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComercioRepository comercioRepository;
    private final ComercioMapper comercioMapper;
    private final com.gocartacho.gocartacho.service.NotificacionService notificacionService;

    @Override
    @Transactional
    public String toggleFavorito(String usuarioId, String comercioId) {
        if (usuarioId == null || comercioId == null) {
            throw new IllegalArgumentException("El usuario o comercio no puede ser nulo");
        }

        Optional<Favorito> favoritoExistente = favoritoRepository.findByUsuarioIdAndComercioId(usuarioId, comercioId);

        if (favoritoExistente.isPresent()) {
            favoritoRepository.deleteById(Objects.requireNonNull(favoritoExistente.get().getId()));
            return "Comercio removido de favoritos";
        } else {
            Usuario usuario = usuarioRepository.findById(Objects.requireNonNull(usuarioId))
                    .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado en DB"));
            Comercio comercio = comercioRepository.findById(Objects.requireNonNull(comercioId))
                    .orElseThrow(() -> new IllegalArgumentException("El comercio indicado no existe"));

            Favorito nuevoFavorito = Favorito.builder()
                    .usuarioId(usuario.getUsuarioId())
                    .comercioId(comercio.getComercioId())
                    .fechaAgregado(LocalDateTime.now())
                    .build();
            favoritoRepository.save(Objects.requireNonNull(nuevoFavorito));

            // Notificar al propietario
            if (comercio.getPropietarioId() != null) {
                usuarioRepository.findById(Objects.requireNonNull(comercio.getPropietarioId())).ifPresent(propietario -> {
                    notificacionService.enviarNotificacion(propietario, "¡Nuevo Favorito!",
                            "Alguien ha añadido tu comercio '" + comercio.getNombre() + "' a sus favoritos.");
                });
            }

            return "Comercio añadido a favoritos";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComercioDTO> obtenerFavoritosUsuario(String usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("El usuarioId no puede ser nulo");
        }

        List<Favorito> favoritos = favoritoRepository.findByUsuarioIdOrderByFechaAgregadoDesc(usuarioId);
        if (favoritos.isEmpty())
            return List.of();

        // OPTIMIZACIÓN: 1 sola query para traer todos los comercios
        List<String> comercioIds = favoritos.stream()
                .map(Favorito::getComercioId)
                .filter(Objects::nonNull)
                .toList();

        Map<String, Comercio> comerciosMap = comercioRepository.findAllById(Objects.requireNonNull(comercioIds))
                .stream()
                .collect(Collectors.toMap(Comercio::getComercioId, c -> c));

        return favoritos.stream()
                .map(fav -> comerciosMap.get(fav.getComercioId()))
                .filter(Objects::nonNull)
                .map(comercioMapper::toDto)
                .toList();
    }
}
