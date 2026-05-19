package com.gocartacho.gocartacho.util;

import com.gocartacho.gocartacho.model.*;
import com.gocartacho.gocartacho.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("dev")
public class DataSeeder {

    private final UsuarioRepository usuarioRepository;
    private final ComercioRepository comercioRepository;
    private final ZonaRepository zonaRepository;
    private final TipoNegocioRepository tipoNegocioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String seed(int numUsuarios, int numComercios) {
        log.info("Iniciando siembra segura de datos simulados (usuarios: {}, comercios: {})...", numUsuarios, numComercios);

        String encodedPassword = passwordEncoder.encode("password123");
        long timestamp = System.currentTimeMillis();

        // 1. Sembrar Usuarios
        int usuariosCreados = 0;
        for (int i = 1; i <= numUsuarios; i++) {
            String suffix = timestamp + "_" + i;
            Usuario user = Usuario.builder()
                    .nombre("Usuario Dev " + i)
                    .apellido("Test")
                    .username("devuser_" + suffix)
                    .email("devuser_" + suffix + "@gocartacho.com")
                    .contrasena(encodedPassword)
                    .rol(RolUsuario.USER)
                    .estado(EstadoUsuario.ACTIVO)
                    .proveedor(ProveedorAuth.LOCAL)
                    .build();
            usuarioRepository.save(java.util.Objects.requireNonNull(user));
            usuariosCreados++;
        }

        // 2. Sembrar Comercios
        List<Zona> zonas = zonaRepository.findAll();
        List<TipoNegocio> tiposNegocio = tipoNegocioRepository.findAll();

        // Fallbacks si no existen zonas o tipos en la base de datos
        Zona fallbackZona = null;
        if (zonas.isEmpty()) {
            fallbackZona = new Zona();
            fallbackZona.setNombre("Zona Dev Temporal");
            fallbackZona.setNumero(999);
            fallbackZona.setLatitud(BigDecimal.valueOf(10.428));
            fallbackZona.setLongitud(BigDecimal.valueOf(-75.549));
            fallbackZona.setDescripcion("Zona temporal de desarrollo");
            zonaRepository.save(java.util.Objects.requireNonNull(fallbackZona));
            zonas = List.of(fallbackZona);
        }

        TipoNegocio fallbackTipo = null;
        if (tiposNegocio.isEmpty()) {
            fallbackTipo = new TipoNegocio();
            fallbackTipo.setNombre("General");
            tipoNegocioRepository.save(java.util.Objects.requireNonNull(fallbackTipo));
            tiposNegocio = List.of(fallbackTipo);
        }

        int comerciosCreados = 0;
        for (int i = 1; i <= numComercios; i++) {
            Zona z = zonas.get(i % zonas.size());
            TipoNegocio t = tiposNegocio.get(i % tiposNegocio.size());

            double latJitter = (Math.random() - 0.5) * 0.01;
            double lonJitter = (Math.random() - 0.5) * 0.01;
            BigDecimal lat = z.getLatitud().add(BigDecimal.valueOf(latJitter));
            BigDecimal lon = z.getLongitud().add(BigDecimal.valueOf(lonJitter));

            Comercio c = Comercio.builder()
                    .nombre("Comercio Dev " + i + " (" + timestamp + ")")
                    .descripcion("Establecimiento de prueba autogenerado.")
                    .direccion("Calle de Prueba " + i)
                    .latitud(lat)
                    .longitud(lon)
                    .ubicacion(new GeoJsonPoint(lon.doubleValue(), lat.doubleValue()))
                    .tipoNegocioId(t.getId())
                    .horarioApertura(LocalTime.of(8, 0))
                    .horarioCierre(LocalTime.of(22, 0))
                    .estadoAprobacion(EstadoComercio.APROBADO)
                    .imagenUrl("https://picsum.photos/seed/devcomercio" + i + "/400/300")
                    .promedioCalificacion(0.0)
                    .totalResenas(0)
                    .zonaId(z.getZonaId())
                    .build();

            comercioRepository.save(java.util.Objects.requireNonNull(c));
            comerciosCreados++;
        }

        log.info("Siembra segura completada: {} usuarios y {} comercios agregados.", usuariosCreados, comerciosCreados);
        return String.format("Se sembraron de forma segura %d usuarios y %d comercios de prueba sin borrar los datos existentes.", 
                usuariosCreados, comerciosCreados);
    }
}
