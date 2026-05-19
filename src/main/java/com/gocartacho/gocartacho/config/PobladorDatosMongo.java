package com.gocartacho.gocartacho.config;

import com.gocartacho.gocartacho.model.PuntoCalor;
import com.gocartacho.gocartacho.model.Resena;
import com.gocartacho.gocartacho.repository.PuntoCalorRepository;
import com.gocartacho.gocartacho.repository.ResenaRepository;
import org.springframework.boot.CommandLineRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Poblador inicial de datos para entidades de analítica y reseñas.
 * Genera puntos de calor simulados y reseñas para demostración.
 * (Desactivado para evitar conflictos con DataLoader)
 */
// @Configuration
public class PobladorDatosMongo implements CommandLineRunner {

    private final PuntoCalorRepository puntoCalorRepository;
    private final ResenaRepository resenaRepository;

    public PobladorDatosMongo(PuntoCalorRepository puntoCalorRepository,
            ResenaRepository resenaRepository) {
        this.puntoCalorRepository = puntoCalorRepository;
        this.resenaRepository = resenaRepository;
    }

    @Override
    public void run(String... args) {
        if (puntoCalorRepository.count() == 0) {
            seedPuntosCalor();
        }
        if (resenaRepository.count() == 0) {
            seedResenas();
        }
    }

    private void seedPuntosCalor() {
        createPunto(10.4235, -75.5509, "hash_001");
        createPunto(10.4240, -75.5515, "hash_002");
        createPunto(10.4215, -75.5518, "hash_003");
        createPunto(10.3985, -75.5602, "hash_004");
    }

    private void createPunto(double lat, double lon, String hash) {
        PuntoCalor p = new PuntoCalor();
        p.setLatitud(BigDecimal.valueOf(lat));
        p.setLongitud(BigDecimal.valueOf(lon));
        p.setTimestamp(LocalDateTime.now());
        p.setDispositivoHash(hash);
        puntoCalorRepository.save(p);
    }

    private void seedResenas() {
        createResena(5, "¡El mejor ceviche que he probado!", "1", "1");
        createResena(4, "Excelente museo, muy conservado.", "4", "2");
    }

    private void createResena(int calificacion, String coment, String uId, String cId) {
        Resena r = new Resena();
        r.setCalificacion(calificacion);
        r.setComentario(coment);
        r.setFecha(LocalDateTime.now());
        r.setUsuarioId(uId);
        r.setComercioId(cId);
        resenaRepository.save(r);
    }
}
