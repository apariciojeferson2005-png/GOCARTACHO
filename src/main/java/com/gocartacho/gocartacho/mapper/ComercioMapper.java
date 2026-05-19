package com.gocartacho.gocartacho.mapper;

import com.gocartacho.gocartacho.dto.ComercioDTO;
import com.gocartacho.gocartacho.model.Comercio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Componente encargado de mapear entre la entidad Comercio y su DTO.
 * Transforma datos crudos y resuelve enumeraciones u objetos complejos (ej: horarios).
 */
@Slf4j
@Component
public class ComercioMapper {

    public Comercio toEntity(ComercioDTO dto) {
        if (dto == null) return null;
        
        Comercio.ComercioBuilder builder = Comercio.builder()
                .comercioId(dto.getId())
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .direccion(dto.getDireccion())
                .tipoNegocioId(dto.getTipoNegocioId())
                .telefono(dto.getTelefono())
                .emailContacto(dto.getEmailContacto())
                .sitioWeb(dto.getSitioWeb())
                .imagenUrl(dto.getImagenUrl())
                .estadoAprobacion(dto.getEstadoAprobacion());

        // Latitud y Longitud
        if (dto.getLatitud() != null) {
            builder.latitud(dto.getLatitud());
        }
        if (dto.getLongitud() != null) {
            builder.longitud(dto.getLongitud());
        }

        // Horarios: se parsean desde String "HH:mm" a LocalTime
        if (dto.getHorarioApertura() != null && !dto.getHorarioApertura().isBlank()) {
            try {
                builder.horarioApertura(LocalTime.parse(dto.getHorarioApertura()));
            } catch (Exception ex) {
                log.warn("Formato de horario de apertura inválido: {}. Motivo: {}", dto.getHorarioApertura(), ex.getMessage());
            }
        }
        if (dto.getHorarioCierre() != null && !dto.getHorarioCierre().isBlank()) {
            try {
                builder.horarioCierre(LocalTime.parse(dto.getHorarioCierre()));
            } catch (Exception ex) {
                log.warn("Formato de horario de cierre inválido: {}. Motivo: {}", dto.getHorarioCierre(), ex.getMessage());
            }
        }

        // Zona: se resuelve desde el ID para establecer la FK correctamente
        String zonaId = dto.getZonaId();
        if (zonaId != null) {
            builder.zonaId(zonaId);
        }

        return builder.build();
    }

    public ComercioDTO toDto(Comercio comercio) {
        if (comercio == null) return null;
        
        ComercioDTO dto = new ComercioDTO();
        dto.setId(comercio.getComercioId());
        dto.setNombre(comercio.getNombre());
        dto.setDescripcion(comercio.getDescripcion());
        dto.setDireccion(comercio.getDireccion());
        dto.setTipoNegocioId(comercio.getTipoNegocioId());
        dto.setTipoNegocio(comercio.getTipoNegocioNombre());
        dto.setTelefono(comercio.getTelefono());
        dto.setEmailContacto(comercio.getEmailContacto());
        dto.setSitioWeb(comercio.getSitioWeb());
        dto.setImagenUrl(comercio.getImagenUrl());
        dto.setEstadoAprobacion(comercio.getEstadoAprobacion());
        dto.setLatitud(comercio.getLatitud());
        dto.setLongitud(comercio.getLongitud());
        dto.setHorarioApertura(comercio.getHorarioApertura() != null ? comercio.getHorarioApertura().toString() : null);
        dto.setHorarioCierre(comercio.getHorarioCierre() != null ? comercio.getHorarioCierre().toString() : null);
        dto.setHorarioAbierto(determinarSiEstaAbierto(comercio.getHorarioApertura(), comercio.getHorarioCierre()));
        // Los campos @Transient se populan por @PostLoad o se sacan de la FK
        dto.setZonaId(comercio.getZonaId());
        dto.setZonaNombre(comercio.getZonaNombre());
        dto.setPropietarioId(comercio.getPropietarioId());
        dto.setPromedioCalificacion(comercio.getPromedioCalificacion());
        dto.setTotalResenas(comercio.getTotalResenas());
        return dto;
    }


    private Boolean determinarSiEstaAbierto(LocalTime apertura, LocalTime cierre) {
        if (apertura == null || cierre == null) {
            return null;
        }
        LocalTime ahora = LocalTime.now(ZoneId.of("America/Bogota"));
        if (cierre.isBefore(apertura)) {
            // Ejemplo: Abre 20:00, Cierra 03:00
            return !ahora.isBefore(apertura) || ahora.isBefore(cierre);
        } else {
            // Ejemplo: Abre 08:00, Cierra 20:00
            return !ahora.isBefore(apertura) && ahora.isBefore(cierre);
        }
    }
}
