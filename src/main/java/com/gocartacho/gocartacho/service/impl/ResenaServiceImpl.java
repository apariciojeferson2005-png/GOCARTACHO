package com.gocartacho.gocartacho.service.impl;

import com.gocartacho.gocartacho.dto.ResenaRequest;
import com.gocartacho.gocartacho.model.Comercio;
import com.gocartacho.gocartacho.model.Resena;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.repository.ComercioRepository;
import com.gocartacho.gocartacho.repository.ResenaRepository;
import com.gocartacho.gocartacho.repository.UsuarioRepository;
import com.gocartacho.gocartacho.model.ReporteResena;
import com.gocartacho.gocartacho.repository.ReporteResenaRepository;
import com.gocartacho.gocartacho.service.ResenaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ResenaServiceImpl implements ResenaService {

    private final ResenaRepository resenaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComercioRepository comercioRepository;
    private final com.gocartacho.gocartacho.repository.PlanRepository planRepository;
    private final com.gocartacho.gocartacho.repository.PlanComercioRepository planComercioRepository;
    private final com.gocartacho.gocartacho.service.NotificacionService notificacionService;
    private final ReporteResenaRepository reporteResenaRepository;

    /**
     * Guarda una nueva reseña. El usuarioId se recibe del controlador que lo toma
     * del
     * contexto de seguridad (usuario autenticado), no del body del request, por
     * seguridad.
     */
    @Override
    @Transactional
    public Resena guardarResena(ResenaRequest request, String usuarioIdAutenticado) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de reseña no puede ser nula");
        }
        if (usuarioIdAutenticado == null) {
            throw new IllegalArgumentException("El usuario autenticado es obligatorio");
        }
        if (request.getComercioId() == null) {
            throw new IllegalArgumentException("El ID del comercio es obligatorio");
        }

        // Validar que el usuario autenticado existe
        Usuario usuario = usuarioRepository.findById(usuarioIdAutenticado)
                .orElseThrow(
                        () -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioIdAutenticado));

        String comId = request.getComercioId();
        if (comId == null)
            throw new IllegalArgumentException("El ID del comercio no puede ser nulo");
        Comercio comercio = comercioRepository.findById(comId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Comercio no encontrado con ID: " + request.getComercioId()));

        Resena nuevaResena = new Resena();
        if (request.getCalificacion() == null)
            throw new IllegalArgumentException("Calificación no puede ser null");
        nuevaResena.setCalificacion(request.getCalificacion());
        nuevaResena.setComentario(request.getComentario());
        nuevaResena.setUsuarioId(usuario.getUsuarioId());
        nuevaResena.setComercioId(comercio.getComercioId());
        nuevaResena.setFecha(LocalDateTime.now());

        Resena resenaGuardada = resenaRepository.save(Objects.requireNonNull(nuevaResena));
        actualizarPromedios(comercio.getComercioId());

        // Notificar al propietario del comercio sobre la nueva reseña
        if (comercio.getPropietarioId() != null) {
            usuarioRepository.findById(Objects.requireNonNull(comercio.getPropietarioId())).ifPresent(propietario -> {
                notificacionService.enviarNotificacion(propietario, "Nueva reseña en " + comercio.getNombre(),
                        "Has recibido una calificación de " + request.getCalificacion() + " estrellas.");
            });
        }

        return resenaGuardada;
    }

    @Override
    public List<Resena> obtenerResenasPorComercio(String comercioId) {
        if (comercioId == null) {
            return Collections.emptyList();
        }
        return comercioRepository.findById(comercioId)
                .map(c -> resenaRepository.findByComercioIdOrderByFechaDesc(c.getComercioId()))
                .orElse(Collections.emptyList());
    }

    @Override
    public Page<Resena> obtenerResenasPorComercioPaginadas(String comercioId, Pageable pageable) {
        Pageable nonNullPageable = (pageable != null) ? pageable : Pageable.unpaged();
        if (comercioId == null) {
            return Page.<Resena>empty(nonNullPageable);
        }
        return resenaRepository.findByComercioId(comercioId, nonNullPageable);
    }

    @Override
    public Double calcularRatingPromedio(String comercioId) {
        if (comercioId == null) {
            return null;
        }
        return resenaRepository.calcularPromedioPorComercioId(comercioId);
    }

    @Override
    @Transactional
    public void eliminarResena(String resenaId, String usuarioId) {
        if (resenaId == null || usuarioId == null) {
            throw new IllegalArgumentException("El ID de la reseña y del usuario no pueden ser nulos");
        }
        Resena resena = resenaRepository.findById(resenaId)
                .orElseThrow(() -> new IllegalArgumentException("Reseña no encontrada"));

        if (!resena.getUsuarioId().equals(usuarioId)) {
            throw new AccessDeniedException("No tienes permiso para borrar esta reseña.");
        }

        String comercioId = resena.getComercioId();
        
        // Borrar reportes en cascada
        reporteResenaRepository.deleteByResenaId(resenaId);
        
        resenaRepository.delete(resena);
        actualizarPromedios(comercioId);
    }

    @Override
    @Transactional
    public void reportarResena(String resenaId) {
        reportarResenaConMotivo(resenaId, "SISTEMA", "Ofensivo/Inapropiado", "Reportado sin cuerpo de detalles adicional.");
    }

    @Override
    @Transactional
    @SuppressWarnings("null")
    public void reportarResenaConMotivo(String resenaId, String usuarioId, String motivo, String detalles) {
        if (resenaId == null) {
            throw new IllegalArgumentException("El ID de la reseña no puede ser nulo");
        }
        Resena resena = resenaRepository.findById(resenaId)
                .orElseThrow(() -> new IllegalArgumentException("Reseña no encontrada"));

        // Marcar la reseña en sí como reportada
        resena.setReportada(true);
        resenaRepository.save(Objects.requireNonNull(resena));

        // Crear y guardar el reporte detallado en reportes_resenas
        ReporteResena reporte = ReporteResena.builder()
                .resenaId(resenaId)
                .usuarioId(usuarioId != null ? usuarioId : "ANÓNIMO")
                .motivo(motivo != null && !motivo.isBlank() ? motivo : "Ofensivo/Inapropiado")
                .detalles(detalles != null && !detalles.isBlank() ? detalles : "Sin detalles adicionales")
                .fecha(LocalDateTime.now())
                .build();
        reporteResenaRepository.save(reporte);
    }

    @Override
    public List<com.gocartacho.gocartacho.dto.ResenaModeracionDTO> obtenerResenasReportadas() {
        List<Resena> reportadas = resenaRepository.findByReportadaTrueOrderByFechaDesc();
        if (reportadas.isEmpty()) {
            return Collections.emptyList();
        }

        // --- OPTIMIZACIÓN N+1 ---
        // 1. Extraer todos los IDs necesarios en una sola pasada.
        Set<String> usuarioIds = reportadas.stream().map(Resena::getUsuarioId).collect(Collectors.toSet());
        Set<String> comercioIds = reportadas.stream().map(Resena::getComercioId).collect(Collectors.toSet());
        List<String> resenaIds = reportadas.stream().map(Resena::getResenaId).toList();

        // 2. Realizar solo consultas en lote a la BD para obtener toda la información.
        Map<String, Usuario> usuariosMap = usuarioRepository.findAllById(Objects.requireNonNull(usuarioIds)).stream()
                .collect(Collectors.toMap(Usuario::getUsuarioId, u -> u));
        Map<String, Comercio> comerciosMap = comercioRepository.findAllById(Objects.requireNonNull(comercioIds))
                .stream()
                .collect(Collectors.toMap(Comercio::getComercioId, c -> c));

        // Cargar todos los reportes de estas reseñas de una sola vez
        List<ReporteResena> todosReportes = reporteResenaRepository.findAll().stream()
                .filter(rep -> resenaIds.contains(rep.getResenaId()))
                .toList();

        Map<String, List<ReporteResena>> reportesPorResena = todosReportes.stream()
                .collect(Collectors.groupingBy(ReporteResena::getResenaId));
        // --- FIN OPTIMIZACIÓN ---

        return reportadas.stream().map(r -> {
            com.gocartacho.gocartacho.dto.ResenaModeracionDTO dto = new com.gocartacho.gocartacho.dto.ResenaModeracionDTO();
            dto.setResenaId(r.getResenaId());
            dto.setComentario(r.getComentario());
            dto.setCalificacion(r.getCalificacion());
            dto.setFecha(r.getFecha());

            // 3. Poblar el DTO usando los Maps en memoria
            Usuario usuario = usuariosMap.get(r.getUsuarioId());
            Comercio comercio = comerciosMap.get(r.getComercioId());

            dto.setUsuarioId(r.getUsuarioId());
            dto.setUsuarioNombre(usuario != null ? usuario.getNombre() : "Usuario no encontrado");
            dto.setUsuarioEmail(usuario != null ? usuario.getEmail() : "N/A");

            dto.setComercioId(r.getComercioId());
            dto.setComercioNombre(comercio != null ? comercio.getNombre() : "Comercio no encontrado");

            // Enriquecer con reportes
            List<ReporteResena> reps = reportesPorResena.getOrDefault(r.getResenaId(), Collections.emptyList());
            dto.setTotalReportes(reps.size());
            
            List<String> detalles = reps.stream()
                    .map(rep -> rep.getMotivo() + ": " + rep.getDetalles())
                    .toList();
            dto.setDetallesReportes(detalles);

            return dto;
        }).toList();
    }

    @Override
    @Transactional
    public void eliminarResenaAdmin(String resenaId) {
        if (resenaId == null)
            throw new IllegalArgumentException("ID nulo");
        Resena resena = resenaRepository.findById(resenaId)
                .orElseThrow(() -> new IllegalArgumentException("Reseña no encontrada"));
        String comercioId = resena.getComercioId();
        
        // Borrar reportes en cascada
        reporteResenaRepository.deleteByResenaId(resenaId);
        
        resenaRepository.deleteById(resenaId);
        actualizarPromedios(comercioId);
    }

    @Override
    @Transactional
    public Resena responderResena(String resenaId, String respuesta, String propietarioId) {
        if (resenaId == null || respuesta == null || propietarioId == null) {
            throw new IllegalArgumentException("Los IDs de reseña, propietario y la respuesta no pueden ser nulos.");
        }

        // 1. Obtener la reseña y el comercio asociado
        Resena resena = resenaRepository.findById(resenaId)
                .orElseThrow(() -> new IllegalArgumentException("Reseña no encontrada."));

        Comercio comercio = comercioRepository.findById(Objects.requireNonNull(resena.getComercioId()))
                .orElseThrow(() -> new IllegalStateException("El comercio asociado a la reseña ya no existe."));

        // 2. Verificación de seguridad: ¿Es el usuario el dueño del comercio?
        if (!propietarioId.equals(comercio.getPropietarioId())) {
            throw new AccessDeniedException("No tienes permiso para responder a reseñas de este comercio.");
        }

        // 3. Actualizar la reseña con la respuesta
        resena.setRespuestaComercio(respuesta);
        resena.setFechaRespuesta(LocalDateTime.now());

        return resenaRepository.save(resena);
    }

    @SuppressWarnings("null")
    private void actualizarPromedios(String comercioId) {
        Double promedio = resenaRepository.calcularPromedioPorComercioId(comercioId);
        long total = resenaRepository.countByComercioId(comercioId);

        Comercio comercio = comercioRepository.findById(Objects.requireNonNull(comercioId)).orElse(null);
        if (comercio != null) {
            comercio.setPromedioCalificacion(promedio != null ? promedio : 0.0);
            comercio.setTotalResenas((int) total);
            comercioRepository.save(comercio);

            List<com.gocartacho.gocartacho.model.PlanComercio> planesComercios = planComercioRepository
                    .findByComercioId(comercioId);
            Set<String> planIds = planesComercios.stream()
                    .map(com.gocartacho.gocartacho.model.PlanComercio::getPlanId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());

            for (String planId : planIds) {
                com.gocartacho.gocartacho.model.Plan plan = planRepository.findById(Objects.requireNonNull(planId))
                        .orElse(null);
                if (plan != null) {
                    List<com.gocartacho.gocartacho.model.PlanComercio> pcs = planComercioRepository
                            .findByPlanIdOrderByOrdenAsc(planId);
                    
                    List<String> comercioIdsPlan = pcs.stream()
                            .map(com.gocartacho.gocartacho.model.PlanComercio::getComercioId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                            
                    Iterable<Comercio> comerciosPlan = comercioRepository.findAllById(comercioIdsPlan);
                    
                    double suma = 0;
                    int count = 0;
                    for (Comercio c : comerciosPlan) {
                        if (c.getPromedioCalificacion() != null) {
                            suma += c.getPromedioCalificacion();
                            count++;
                        }
                    }
                    plan.setPromedioCalificacion(count > 0 ? suma / count : 0.0);
                    planRepository.save(plan);
                }
            }
        }
    }

    @Override
    @Transactional
    public void descartarReporteResena(String resenaId) {
        if (resenaId == null)
            throw new IllegalArgumentException("El ID de la reseña no puede ser nulo");
        Resena resena = resenaRepository.findById(resenaId)
                .orElseThrow(() -> new IllegalArgumentException("Reseña no encontrada"));
        resena.setReportada(false);
        resenaRepository.save(Objects.requireNonNull(resena));
        
        // Limpiar reportes al ser descartados/aprobados
        reporteResenaRepository.deleteByResenaId(resenaId);
    }

    @Override
    public long contarResenas() {
        return resenaRepository.count();
    }

    @Override
    public Resena obtenerResenaPorId(String id) {
        if (id == null) return null;
        return resenaRepository.findById(id).orElse(null);
    }
}