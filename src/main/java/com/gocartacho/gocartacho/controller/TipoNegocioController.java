package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.dto.TipoNegocioDTO;
import com.gocartacho.gocartacho.model.TipoNegocio;
import com.gocartacho.gocartacho.service.TipoNegocioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tipos-negocio")
@RequiredArgsConstructor
@Tag(name = "Tipo Negocio", description = "Endpoints para la gestión de tipos de negocio")
public class TipoNegocioController {

    private final TipoNegocioService tipoNegocioService;

    @GetMapping
    @Operation(summary = "Listar todos los tipos de negocio")
    public ResponseEntity<List<TipoNegocioDTO>> listarTodos() {
        List<TipoNegocioDTO> tipos = tipoNegocioService.listarTodos().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tipos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un tipo de negocio por ID")
    public ResponseEntity<TipoNegocioDTO> obtenerPorId(@PathVariable Long id) {
        return tipoNegocioService.obtenerPorId(id)
                .map(tipo -> ResponseEntity.ok(convertToDTO(tipo)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un nuevo tipo de negocio (Solo ADMIN)")
    public ResponseEntity<TipoNegocioDTO> crear(@RequestBody TipoNegocioDTO dto) {
        TipoNegocio tipo = TipoNegocio.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .build();
        return ResponseEntity.ok(convertToDTO(tipoNegocioService.guardar(tipo)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar un tipo de negocio (Solo ADMIN)")
    public ResponseEntity<TipoNegocioDTO> actualizar(@PathVariable Long id, @RequestBody TipoNegocioDTO dto) {
        return tipoNegocioService.obtenerPorId(id)
                .map(tipo -> {
                    tipo.setNombre(dto.getNombre());
                    tipo.setDescripcion(dto.getDescripcion());
                    return ResponseEntity.ok(convertToDTO(tipoNegocioService.guardar(tipo)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar un tipo de negocio (Solo ADMIN)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tipoNegocioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private TipoNegocioDTO convertToDTO(TipoNegocio tipo) {
        return TipoNegocioDTO.builder()
                .id(tipo.getId())
                .nombre(tipo.getNombre())
                .descripcion(tipo.getDescripcion())
                .build();
    }
}
