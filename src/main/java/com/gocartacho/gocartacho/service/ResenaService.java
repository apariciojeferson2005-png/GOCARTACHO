package com.gocartacho.gocartacho.service;

import com.gocartacho.gocartacho.dto.ResenaModeracionDTO;
import com.gocartacho.gocartacho.dto.ResenaRequest;
import com.gocartacho.gocartacho.model.Resena;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ResenaService {

    /**
     * Guarda una nueva reseña. El usuarioId se toma del usuario autenticado, no del
     * body.
     */
    Resena guardarResena(ResenaRequest request, String usuarioIdAutenticado);

    /**
     * Obtiene todas las reseñas para un comercio específico (sin paginación,
     * compatibilidad).
     */
    List<Resena> obtenerResenasPorComercio(String comercioId);

    /**
     * Obtiene reseñas paginadas para un comercio específico.
     */
    Page<Resena> obtenerResenasPorComercioPaginadas(String comercioId, Pageable pageable);

    /**
     * Calcula el rating promedio de un comercio basado en sus reseñas.
     * Retorna null si no hay reseñas.
     */
    Double calcularRatingPromedio(String comercioId);

    /**
     * Elimina una reseña asegurando que pertenezca al usuario indicado.
     */
    void eliminarResena(String resenaId, String usuarioId);

    /**
     * Marca una reseña como reportada.
     */
    void reportarResena(String resenaId);

    /**
     * Reporta una reseña asociando un usuario, motivo y detalles específicos.
     */
    void reportarResenaConMotivo(String resenaId, String usuarioId, String motivo, String detalles);

    /**
     * Obtiene la lista de todas las reseñas que han sido reportadas, con detalles
     * del autor y comercio.
     */
    List<ResenaModeracionDTO> obtenerResenasReportadas();

    /**
     * Elimina una reseña sin verificar el usuario (uso exclusivo ADMIN).
     */
    void eliminarResenaAdmin(String resenaId);

    /**
     * Permite al dueño de un comercio responder a una reseña.
     * Verifica que el usuario autenticado sea el propietario del comercio asociado
     * a la reseña.
     */
    Resena responderResena(String resenaId, String respuesta, String propietarioId);

    /**
     * Quita la marca de reportada a una reseña (desestima el reporte).
     */
    void descartarReporteResena(String resenaId);

    /**
     * Cuenta el total de reseñas en la base de datos (para estadísticas del administrador).
     */
    long contarResenas();

    /**
     * Obtiene una reseña por su identificador único.
     */
    Resena obtenerResenaPorId(String id);
}