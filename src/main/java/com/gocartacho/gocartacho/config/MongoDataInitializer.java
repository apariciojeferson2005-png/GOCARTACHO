package com.gocartacho.gocartacho.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gocartacho.gocartacho.model.*;
import com.gocartacho.gocartacho.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Inicializador de datos para MongoDB.
 * Carga los datos iniciales desde un archivo JSON en lugar de tenerlos hardcodeados.
 * (Desactivado para evitar conflictos con DataLoader)
 */
@Slf4j
// @Configuration
@RequiredArgsConstructor
public class MongoDataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final ZonaRepository zonaRepository;
    private final ComercioRepository comercioRepository;
    private final PlanRepository planRepository;
    private final PlanComercioRepository planComercioRepository;
    private final PromocionRepository promocionRepository;
    private final com.gocartacho.gocartacho.service.TipoNegocioService tipoNegocioService;
    private final PasswordEncoder passwordEncoder;
    private final ResourceLoader resourceLoader;

    @Override
    public void run(String... args) throws Exception {
        if (usuarioRepository.count() == 0 && zonaRepository.count() == 0) {
            log.info("Iniciando carga de datos desde initial-data.json...");

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            Resource resource = resourceLoader.getResource("classpath:initial-data.json");
            try (InputStream inputStream = resource.getInputStream()) {
                Map<String, List<Map<String, Object>>> data = mapper.readValue(inputStream, new TypeReference<>() {});

                // 1. Cargar Usuarios
                List<Usuario> usuarios = mapper.convertValue(data.get("usuarios"), new TypeReference<List<Usuario>>() {});
                if (usuarios != null && !usuarios.isEmpty()) {
                    String defaultPwd = passwordEncoder.encode("password");
                    usuarios.forEach(u -> u.setContrasena(defaultPwd));
                    usuarioRepository.saveAll(usuarios);
                }

                // 2. Cargar Zonas
                List<Zona> zonas = mapper.convertValue(data.get("zonas"), new TypeReference<List<Zona>>() {});
                if (zonas != null && !zonas.isEmpty()) {
                    zonaRepository.saveAll(zonas);
                }

                // 3. Cargar Comercios (Con resolución de TipoNegocioId desde MySQL)
                List<Map<String, Object>> comerciosRaw = data.get("comercios");
                if (comerciosRaw != null && !comerciosRaw.isEmpty()) {
                    List<Comercio> comercios = new java.util.ArrayList<>();
                    for (Map<String, Object> raw : comerciosRaw) {
                        Comercio c = mapper.convertValue(raw, Comercio.class);
                        String tipoNombre = (String) raw.get("tipoNegocio");
                        if (tipoNombre != null) {
                            tipoNegocioService.obtenerPorNombre(tipoNombre)
                                    .ifPresent(tn -> c.setTipoNegocioId(tn.getId()));
                        }
                        comercios.add(c);
                    }
                    comercioRepository.saveAll(comercios);
                }

                // 4. Cargar Planes
                List<Plan> planes = mapper.convertValue(data.get("planes"), new TypeReference<List<Plan>>() {});
                if (planes != null && !planes.isEmpty()) {
                    planRepository.saveAll(planes);
                }

                // 5. Cargar PlanesComercios
                List<PlanComercio> planesComercios = mapper.convertValue(data.get("planesComercios"), new TypeReference<List<PlanComercio>>() {});
                if (planesComercios != null && !planesComercios.isEmpty()) {
                    planComercioRepository.saveAll(planesComercios);
                }

                // 6. Cargar Promociones
                List<Promocion> promociones = mapper.convertValue(data.get("promociones"), new TypeReference<List<Promocion>>() {});
                if (promociones != null && !promociones.isEmpty()) {
                    promocionRepository.saveAll(promociones);
                }

                int numComercios = (comerciosRaw != null) ? comerciosRaw.size() : 0;
                int numZonas = (zonas != null) ? zonas.size() : 0;
                log.info("✅ MongoDB inicializado exitosamente con {} comercios y {} zonas.", numComercios, numZonas);
            } catch (Exception e) {
                log.error("❌ Error al cargar datos iniciales: {}", e.getMessage(), e);
            }
        }
    }
}
