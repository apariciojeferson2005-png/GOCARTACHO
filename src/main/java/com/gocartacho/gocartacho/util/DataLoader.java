package com.gocartacho.gocartacho.util;

import com.gocartacho.gocartacho.model.RolUsuario;
import com.gocartacho.gocartacho.model.Usuario;
import com.gocartacho.gocartacho.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cargador de datos inicial "a la antigua".
 * Se ejecuta automáticamente al arrancar la aplicación.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final com.gocartacho.gocartacho.repository.ZonaRepository zonaRepository;
    private final com.gocartacho.gocartacho.repository.ComercioRepository comercioRepository;
    private final com.gocartacho.gocartacho.repository.TipoNegocioRepository tipoNegocioRepository;
    private final com.gocartacho.gocartacho.repository.PromocionRepository promocionRepository;
    private final com.gocartacho.gocartacho.repository.PlanRepository planRepository;
    private final com.gocartacho.gocartacho.repository.PlanComercioRepository planComercioRepository;
    private final com.gocartacho.gocartacho.repository.ResenaRepository resenaRepository;
    private final com.gocartacho.gocartacho.repository.FavoritoRepository favoritoRepository;
    private final com.gocartacho.gocartacho.repository.PuntoCalorRepository puntoCalorRepository;
    private final com.gocartacho.gocartacho.repository.AfluenciaHistoricaRepository afluenciaHistoricaRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.cache.CacheManager cacheManager;

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando carga de datos de prueba...");

        // 1. Usuarios (10)
        inicializarUsuarios();

        // 2. Zonas (Ya existentes, no se tocan según petición)
        inicializarZonas();

        // 3. Tipos de Negocio (10)
        inicializarTiposNegocio();

        // 4. Comercios (10)
        inicializarComercios();

        // 5. Promociones (10)
        inicializarPromociones();

        // 6. Planes (10)
        inicializarPlanes();

        // 6b. Relaciones Plan-Comercio
        inicializarPlanComercios();

        // 7. Reseñas (10)
        inicializarResenas();

        // 8. Favoritos (10)
        inicializarFavoritos();

        // 9. Puntos de Calor (Active users matching business coordinates)
        inicializarPuntosCalor();
        
        // 9b. Afluencia Histórica (Real generated ObjectIds)
        inicializarAfluenciaHistorica();

        // 10. Migración de reseñas huérfanas
        migrarResenasExistentes();

        // LIMPIAR CACHÉ para que los cambios se vean reflejados
        org.springframework.cache.Cache cacheComercios = cacheManager != null ? cacheManager.getCache("comercios")
                : null;
        if (cacheComercios != null) {
            cacheComercios.clear();
            log.info("Caché de comercios limpiada.");
        }

        log.info("Carga de datos de prueba finalizada.");
    }

    private void inicializarUsuarios() {
        if (usuarioRepository.count() >= 10)
            return;
        log.info("Cargando usuarios de prueba...");

        String password = passwordEncoder.encode("password123");

        // Admin maestro
        if (usuarioRepository.findByUsername("admin").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setNombre("Admin");
            admin.setApellido("Master");
            admin.setUsername("admin");
            admin.setEmail("admin@gocartacho.com");
            admin.setContrasena(password);
            admin.setRol(RolUsuario.SUPER_ADMIN);
            usuarioRepository.save(admin);
        }

        // Comerciante de prueba
        if (usuarioRepository.findByUsername("comerciante").isEmpty()) {
            Usuario comerciante = new Usuario();
            comerciante.setNombre("Dueño");
            comerciante.setApellido("Local");
            comerciante.setUsername("comerciante");
            comerciante.setEmail("comerciante@gocartacho.com");
            comerciante.setContrasena(password);
            comerciante.setRol(RolUsuario.COMERCIANTE);
            usuarioRepository.save(comerciante);
        }

        for (int i = 1; i <= 9; i++) {
            String username = "user" + i;
            if (usuarioRepository.findByUsername(username).isEmpty()) {
                Usuario user = new Usuario();
                user.setNombre("Usuario " + i);
                user.setApellido("Prueba");
                user.setUsername(username);
                user.setEmail(username + "@test.com");
                user.setContrasena(password);
                user.setRol(RolUsuario.USER);
                usuarioRepository.save(user);
            }
        }
    }

    private void inicializarZonas() {
        if (zonaRepository.count() > 0)
            return;
        log.info("Inicializando zonas geográficas...");
        insertarZona("Centro Histórico", 1, 10.428, -75.549, "El corazón histórico.");
        insertarZona("Getsemaní", 2, 10.422, -75.548, "Barrio bohemio.");
        insertarZona("Bocagrande", 3, 10.418, -75.552, "Zona moderna.");
        insertarZona("Castillogrande", 4, 10.395, -75.558, "Área residencial exclusiva.");
        insertarZona("Manga", 5, 10.417, -75.542, "Barrio de mansiones.");
        insertarZona("San Diego", 7, 10.431, -75.545, "Elegancia histórica.");
        insertarZona("La Matuna", 8, 10.426, -75.544, "Centro financiero.");
    }

    private void inicializarTiposNegocio() {
        if (tipoNegocioRepository.count() >= 10)
            return;
        log.info("Cargando tipos de negocio...");
        String[] tipos = { "Restaurante", "Bar", "Museo", "Hotel", "Tienda", "Cafetería", "Discoteca", "Gastrobar",
                "Centro Cultural", "Artesanías" };
        for (String t : tipos) {
            if (tipoNegocioRepository.findByNombreIgnoreCase(t).isEmpty()) {
                com.gocartacho.gocartacho.model.TipoNegocio tn = new com.gocartacho.gocartacho.model.TipoNegocio();
                tn.setNombre(t);
                tn.setDescripcion("Categoría de " + t);
                tipoNegocioRepository.save(tn);
            }
        }
    }

    private void inicializarComercios() {
        // Lógica de reparación para comercios que ya existen pero no tienen ubicación o
        // imagen
        java.util.List<com.gocartacho.gocartacho.model.Comercio> existentes = comercioRepository.findAll();
        Usuario admin = usuarioRepository.findByUsername("admin").orElse(null);

        for (com.gocartacho.gocartacho.model.Comercio c : existentes) {
            boolean modificado = false;
            if (c.getUbicacion() == null && c.getLatitud() != null && c.getLongitud() != null) {
                c.setUbicacion(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(c.getLongitud().doubleValue(),
                        c.getLatitud().doubleValue()));
                modificado = true;
            }
            if (c.getImagenUrl() == null || c.getImagenUrl().isEmpty()) {
                c.setImagenUrl("https://picsum.photos/seed/" + c.getNombre().replace(" ", "") + "/400/300");
                modificado = true;
            }
            if (c.getHorarioApertura() == null) {
                c.setHorarioApertura(java.time.LocalTime.of(9, 0));
                modificado = true;
            }
            if (c.getHorarioCierre() == null) {
                c.setHorarioCierre(java.time.LocalTime.of(22, 0));
                modificado = true;
            }
            if (modificado) {
                comercioRepository.save(c);
            }
        }

        if (comercioRepository.count() >= 10)
            return;
        log.info("Cargando comercios de prueba...");
        java.util.List<com.gocartacho.gocartacho.model.Zona> zonas = zonaRepository.findAll();
        java.util.List<com.gocartacho.gocartacho.model.TipoNegocio> tipos = tipoNegocioRepository.findAll();

        String[] nombres = { "La Mulata", "Cafe del Mar", "Alquímico", "El Barón", "Carmen", "Marea", "Celele",
                "Townhouse", "Demente", "Mirador" };
        for (int i = 0; i < 10; i++) {
            com.gocartacho.gocartacho.model.Zona z = zonas.get(i % zonas.size());
            com.gocartacho.gocartacho.model.TipoNegocio t = tipos.get(i % tipos.size());

            com.gocartacho.gocartacho.model.Comercio c = new com.gocartacho.gocartacho.model.Comercio();
            c.setNombre(nombres[i]);
            c.setDescripcion("Descripción del comercio " + nombres[i]);
            c.setDireccion("Calle Falsa 123, Cartagena");
            c.setHorarioApertura(java.time.LocalTime.of(8 + (i % 3), 0));
            c.setHorarioCierre(java.time.LocalTime.of(20 + (i % 4), 0));

            java.math.BigDecimal lat = z.getLatitud().add(java.math.BigDecimal.valueOf(0.001 * i));
            java.math.BigDecimal lon = z.getLongitud().add(java.math.BigDecimal.valueOf(0.001 * i));

            c.setLatitud(lat);
            c.setLongitud(lon);
            // IMPORTANTE: Inicializar la ubicación geoespacial para que MongoDB pueda
            // indexarlos
            c.setUbicacion(
                    new org.springframework.data.mongodb.core.geo.GeoJsonPoint(lon.doubleValue(), lat.doubleValue()));

            c.setTipoNegocioId(t.getId());
            c.setZonaId(z.getZonaId());
            c.setPropietarioId(admin != null ? admin.getUsuarioId() : null);
            c.setEstadoAprobacion(com.gocartacho.gocartacho.model.EstadoComercio.APROBADO);
            c.setImagenUrl("https://picsum.photos/seed/" + nombres[i].replace(" ", "") + "/400/300");
            comercioRepository.save(c);
        }
    }

    private void inicializarPromociones() {
        if (promocionRepository.count() > 0) {
            log.info("Ya existen promociones en la base de datos. Saltando inicialización.");
            return;
        }
        log.info("Cargando promociones...");
        java.util.List<com.gocartacho.gocartacho.model.Comercio> comercios = comercioRepository.findAll();
        for (int i = 0; i < 10; i++) {
            com.gocartacho.gocartacho.model.Comercio c = comercios.get(i % comercios.size());
            com.gocartacho.gocartacho.model.Promocion p = new com.gocartacho.gocartacho.model.Promocion();
            p.setTitulo("Descuento en " + c.getNombre());
            p.setDescripcion("Aprovecha un 20% de descuento este fin de semana.");
            p.setPorcentajeDescuento(java.math.BigDecimal.valueOf(20));
            p.setFechaInicio(java.time.LocalDate.now());
            p.setFechaFin(java.time.LocalDate.now().plusDays(30));
            p.setActiva(true);
            p.setComercioId(c.getComercioId());
            promocionRepository.save(p);
        }
    }

    private void inicializarPlanes() {
        if (planRepository.count() >= 10)
            return;
        log.info("Cargando planes turísticos...");
        String[] planes = { "Ruta Histórica", "Tour Gastronómico", "Planes Románticos", "Noche de Bares",
                "Cultura y Museos", "Bahía y Atardecer", "Getsemaní Experience", "Planes Familiares",
                "Ruta de Murallas", "Cartagena Moderna" };
        for (String p : planes) {
            com.gocartacho.gocartacho.model.Plan plan = new com.gocartacho.gocartacho.model.Plan();
            plan.setNombrePlan(p);
            plan.setDescripcion("Una experiencia única diseñada para disfrutar " + p);
            plan.setPromedioCalificacion(4.5);
            planRepository.save(plan);
        }
    }

    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra en kilómetros
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void inicializarPlanComercios() {
        if (planComercioRepository.count() > 0) {
            log.info("Ya existen relaciones plan-comercio en la base de datos. Saltando inicialización.");
            return;
        }
        log.info("Cargando relaciones plan-comercio...");

        java.util.List<com.gocartacho.gocartacho.model.Plan> planes = planRepository.findAll();
        java.util.List<com.gocartacho.gocartacho.model.Comercio> comercios = comercioRepository.findAll();

        if (planes.isEmpty() || comercios.isEmpty()) {
            log.warn("No hay planes o comercios para crear relaciones plan-comercio.");
            return;
        }

        int planIdx = 0;
        for (com.gocartacho.gocartacho.model.Plan plan : planes) {
            com.gocartacho.gocartacho.model.Comercio cBase = comercios.get(planIdx % comercios.size());

            // Ordenar todos los comercios por cercanía a cBase
            java.util.List<com.gocartacho.gocartacho.model.Comercio> ordenadosPorDistancia = new java.util.ArrayList<>(comercios);
            ordenadosPorDistancia.sort((c1, c2) -> {
                double latBase = cBase.getLatitud() != null ? cBase.getLatitud().doubleValue() : 0.0;
                double lonBase = cBase.getLongitud() != null ? cBase.getLongitud().doubleValue() : 0.0;

                double lat1 = c1.getLatitud() != null ? c1.getLatitud().doubleValue() : 0.0;
                double lon1 = c1.getLongitud() != null ? c1.getLongitud().doubleValue() : 0.0;

                double lat2 = c2.getLatitud() != null ? c2.getLatitud().doubleValue() : 0.0;
                double lon2 = c2.getLongitud() != null ? c2.getLongitud().doubleValue() : 0.0;

                double dist1 = calcularDistancia(latBase, lonBase, lat1, lon1);
                double dist2 = calcularDistancia(latBase, lonBase, lat2, lon2);

                return Double.compare(dist1, dist2);
            });

            // Tomar los 3 comercios más cercanos (incluyendo cBase que estará en la posición 0 con distancia 0.0)
            int orden = 1;
            for (int i = 0; i < Math.min(3, ordenadosPorDistancia.size()); i++) {
                com.gocartacho.gocartacho.model.Comercio c = ordenadosPorDistancia.get(i);
                com.gocartacho.gocartacho.model.PlanComercio pc = new com.gocartacho.gocartacho.model.PlanComercio();
                pc.setPlanId(plan.getPlanId());
                pc.setComercioId(c.getComercioId());
                pc.setOrden(orden);
                pc.setRecomendacion(obtenerRecomendacionParaComercio(c.getNombre(), orden));
                planComercioRepository.save(pc);
                orden++;
            }
            planIdx++;
        }
        log.info("Relaciones plan-comercio creadas: {} registros.", planes.size() * 3);
    }

    private String obtenerRecomendacionParaComercio(String nombreComercio, int orden) {
        if (nombreComercio == null) return "Disfruta de la gran atmósfera de este establecimiento.";
        
        switch (nombreComercio) {
            case "La Mulata":
                return "ve primero a La Mulata porque sus chicharrones son crujientes, deliciosos y los acompañan con arepa de huevo cartagenera.";
            case "Cafe del Mar":
                return "visita Cafe del Mar para ver un atardecer mágico sobre las murallas históricas con una vista espectacular del océano.";
            case "Alquímico":
                return "pasa a Alquímico por un cóctel de autor innovador en su terraza tropical, elegida entre las mejores del mundo.";
            case "El Barón":
                return "haz una parada en El Barón por su café premium frío o un cóctel clásico frente a la histórica Plaza de San Pedro Claver.";
            case "Carmen":
                return "deléitate en Carmen con una cena gourmet de alta cocina colombiana en su hermoso patio colonial iluminado.";
            case "Marea":
                return "acércate a Marea por sus mariscos frescos y pescados con una vista inmejorable del Centro de Convenciones.";
            case "Celele":
                return "prueba la cocina caribeña contemporánea en Celele, un restaurante galardonado que reinventa los sabores locales.";
            case "Townhouse":
                return "sube al rooftop de Townhouse para disfrutar de música increíble, buena vibra y tapas deliciosas a cielo abierto.";
            case "Demente":
                return "relájate en Demente (en Getsemaní) saboreando sus pizzas artesanales al horno de leña y cervezas bien heladas.";
            case "Mirador":
                return "ve al Mirador para capturar las mejores fotos panorámicas de la Torre del Reloj iluminada por la noche.";
            default:
                if (orden == 1) {
                    return "empieza tu recorrido aquí degustando los aperitivos locales y disfrutando de un ambiente acogedor.";
                } else if (orden == 2) {
                    return "sigue el trayecto en este establecimiento para recargar energías con un postre o bebida refrescante.";
                } else {
                    return "finaliza el recorrido en este lugar para relajarte y contemplar la vida nocturna cartagenera.";
                }
        }
    }

    private void inicializarResenas() {
        if (resenaRepository.count() >= 10)
            return;
        log.info("Cargando reseñas...");
        java.util.List<Usuario> usuarios = usuarioRepository.findAll();
        java.util.List<com.gocartacho.gocartacho.model.Comercio> comercios = comercioRepository.findAll();
        for (int i = 0; i < 10; i++) {
            Usuario u = usuarios.get(i % usuarios.size());
            com.gocartacho.gocartacho.model.Comercio c = comercios.get(i % comercios.size());

            com.gocartacho.gocartacho.model.Resena r = new com.gocartacho.gocartacho.model.Resena();
            r.setCalificacion(5);
            r.setComentario("Excelente lugar, muy recomendado. " + i);
            r.setFecha(java.time.LocalDateTime.now());
            r.setUsuarioId(u.getUsuarioId());
            r.setComercioId(c.getComercioId());
            resenaRepository.save(r);
        }
    }

    private void inicializarFavoritos() {
        if (favoritoRepository.count() >= 10)
            return;
        log.info("Cargando favoritos...");
        java.util.List<Usuario> usuarios = usuarioRepository.findAll();
        java.util.List<com.gocartacho.gocartacho.model.Comercio> comercios = comercioRepository.findAll();
        for (int i = 0; i < 10; i++) {
            Usuario u = usuarios.get(i % usuarios.size());
            com.gocartacho.gocartacho.model.Comercio c = comercios.get(i % comercios.size());

            com.gocartacho.gocartacho.model.Favorito f = new com.gocartacho.gocartacho.model.Favorito();
            f.setUsuarioId(u.getUsuarioId());
            f.setComercioId(c.getComercioId());
            f.setFechaAgregado(java.time.LocalDateTime.now());
            favoritoRepository.save(f);
        }
    }

    private void insertarZona(String nombre, int numero, double lat, double lon, String desc) {
        com.gocartacho.gocartacho.model.Zona zona = new com.gocartacho.gocartacho.model.Zona();
        zona.setNombre(nombre);
        zona.setNumero(numero);
        zona.setLatitud(java.math.BigDecimal.valueOf(lat));
        zona.setLongitud(java.math.BigDecimal.valueOf(lon));
        zona.setDescripcion(desc);
        zonaRepository.save(zona);
    }

    private void inicializarPuntosCalor() {
        if (puntoCalorRepository.count() > 0) {
            log.info("Ya existen puntos de calor en la base de datos. Saltando inicialización.");
            return;
        }
        log.info("Inicializando puntos de calor simulados (usuarios activos dispersos alrededor de los comercios)...");
        java.util.List<com.gocartacho.gocartacho.model.Comercio> comercios = comercioRepository.findAll();
        int devIndex = 1;
        for (com.gocartacho.gocartacho.model.Comercio c : comercios) {
            if (c.getLatitud() != null && c.getLongitud() != null) {
                // Generar entre 1 y 3 usuarios activos dispersos alrededor de cada comercio
                int numPuntos = 1 + (int)(Math.random() * 3);
                for (int i = 0; i < numPuntos; i++) {
                    com.gocartacho.gocartacho.model.PuntoCalor pc = new com.gocartacho.gocartacho.model.PuntoCalor();
                    // Agregar una dispersión aleatoria realista (+/- 80 metros)
                    double latJitter = (Math.random() - 0.5) * 0.0015;
                    double lngJitter = (Math.random() - 0.5) * 0.0015;
                    pc.setLatitud(c.getLatitud().add(java.math.BigDecimal.valueOf(latJitter)));
                    pc.setLongitud(c.getLongitud().add(java.math.BigDecimal.valueOf(lngJitter)));
                    pc.setUbicacion(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(
                            pc.getLongitud().doubleValue(), pc.getLatitud().doubleValue()));
                    pc.setTimestamp(java.time.LocalDateTime.now());
                    pc.setDispositivoHash("user_device_" + devIndex);
                    puntoCalorRepository.save(pc);
                    devIndex++;
                }
            }
        }
        log.info("Puntos de calor dispersos inicializados: {} registros.", devIndex - 1);
    }

    private void migrarResenasExistentes() {
        log.info("Iniciando migración y vinculación de reseñas huérfanas en la base de datos...");

        java.util.List<com.gocartacho.gocartacho.model.Resena> todasResenas = resenaRepository.findAll();
        if (todasResenas.isEmpty()) {
            return;
        }

        // Obtener comercios de la base de datos
        java.util.List<com.gocartacho.gocartacho.model.Comercio> comercios = comercioRepository.findAll();
        // Obtener usuarios de la base de datos
        java.util.List<Usuario> usuarios = usuarioRepository.findAll();

        // Mapeo de nombres de comercios viejos a sus entidades actuales
        java.util.Map<String, com.gocartacho.gocartacho.model.Comercio> comerciosMap = new java.util.HashMap<>();
        for (com.gocartacho.gocartacho.model.Comercio c : comercios) {
            comerciosMap.put(c.getNombre().toLowerCase().trim(), c);
        }

        // Mapeo de usuarios por su nombre de usuario
        java.util.Map<String, Usuario> usuariosMap = new java.util.HashMap<>();
        for (Usuario u : usuarios) {
            usuariosMap.put(u.getUsername().toLowerCase().trim(), u);
        }

        // Mapeos de IDs viejos
        String[] nombresComerciosViejos = { "La Mulata", "Cafe del Mar", "Alquímico", "El Barón", "Carmen", "Marea",
                "Celele", "Townhouse", "Demente", "Mirador" };
        String[] nombresUsuariosViejos = { "admin", "user1", "user2", "user3", "user4", "user5", "user6", "user7",
                "user8", "user9" };

        int migradosCount = 0;

        for (com.gocartacho.gocartacho.model.Resena r : todasResenas) {
            boolean modificado = false;

            String oldComercioId = r.getComercioId();
            String oldUsuarioId = r.getUsuarioId();

            // Si es un ID viejo de comercio (es un número del "1" al "10")
            if (oldComercioId != null && oldComercioId.matches("\\d+")) {
                int index = Integer.parseInt(oldComercioId) - 1;
                if (index >= 0 && index < nombresComerciosViejos.length) {
                    String nombre = nombresComerciosViejos[index];
                    com.gocartacho.gocartacho.model.Comercio realCom = comerciosMap.get(nombre.toLowerCase().trim());
                    if (realCom != null) {
                        r.setComercioId(realCom.getComercioId());
                        modificado = true;
                    }
                }
            }

            // Si es un ID viejo de usuario (es un número del "1" al "10")
            if (oldUsuarioId != null && oldUsuarioId.matches("\\d+")) {
                int index = Integer.parseInt(oldUsuarioId) - 1;
                if (index >= 0 && index < nombresUsuariosViejos.length) {
                    String username = nombresUsuariosViejos[index];
                    Usuario realUser = usuariosMap.get(username.toLowerCase().trim());
                    if (realUser != null) {
                        r.setUsuarioId(realUser.getUsuarioId());
                        modificado = true;
                    }
                }
            }

            if (modificado) {
                resenaRepository.save(r);
                migradosCount++;
            }
        }

        log.info("Migración de reseñas finalizada. Se actualizaron {} reseñas huérfanas.", migradosCount);

        // Recalcular promedios para todos los comercios después de migrar
        for (com.gocartacho.gocartacho.model.Comercio c : comercios) {
            actualizarPromediosParaComercio(c.getComercioId());
        }
    }

    private void actualizarPromediosParaComercio(String comercioId) {
        if (comercioId == null)
            return;
        Double promedio = resenaRepository.calcularPromedioPorComercioId(comercioId);
        long total = resenaRepository.countByComercioId(comercioId);

        com.gocartacho.gocartacho.model.Comercio comercio = comercioRepository.findById(comercioId).orElse(null);
        if (comercio != null) {
            comercio.setPromedioCalificacion(promedio != null ? promedio : 0.0);
            comercio.setTotalResenas((int) total);
            comercioRepository.save(comercio);
            log.info("Promedios actualizados para comercio {}: promedio={}, total={}", comercio.getNombre(), promedio,
                    total);
        }

        // Actualizar planes que contienen este comercio
        java.util.List<com.gocartacho.gocartacho.model.PlanComercio> planesComercios = planComercioRepository
                .findByComercioId(comercioId);
        java.util.Set<String> planIds = new java.util.HashSet<>();
        for (com.gocartacho.gocartacho.model.PlanComercio pc : planesComercios) {
            if (pc.getPlanId() != null) {
                planIds.add(pc.getPlanId());
            }
        }

        for (String planId : planIds) {
            com.gocartacho.gocartacho.model.Plan plan = planRepository
                    .findById(java.util.Objects.requireNonNull(planId)).orElse(null);
            if (plan != null) {
                java.util.List<com.gocartacho.gocartacho.model.PlanComercio> pcs = planComercioRepository
                        .findByPlanIdOrderByOrdenAsc(planId);
                double suma = 0;
                int count = 0;
                for (com.gocartacho.gocartacho.model.PlanComercio pc : pcs) {
                    String pcComercioId = pc.getComercioId();
                    if (pcComercioId != null) {
                        com.gocartacho.gocartacho.model.Comercio c = comercioRepository.findById(pcComercioId)
                                .orElse(null);
                        if (c != null && c.getPromedioCalificacion() != null) {
                            suma += c.getPromedioCalificacion();
                            count++;
                        }
                    }
                }
                plan.setPromedioCalificacion(count > 0 ? suma / count : 0.0);
                planRepository.save(plan);
            }
        }
    }

    private void inicializarAfluenciaHistorica() {
        if (afluenciaHistoricaRepository.count() > 0) {
            log.info("Ya existe afluencia histórica en la base de datos. Saltando inicialización.");
            return;
        }
        log.info("Inicializando afluencia histórica para las zonas...");
        java.util.List<com.gocartacho.gocartacho.model.Zona> zonas = zonaRepository.findAll();
        for (com.gocartacho.gocartacho.model.Zona zona : zonas) {
            String zId = zona.getZonaId();
            if (zId == null) continue;

            // Lunes
            createAfluencia(zId, com.gocartacho.gocartacho.model.DiaSemana.Lunes, 10, com.gocartacho.gocartacho.model.NivelAfluencia.Bajo);
            createAfluencia(zId, com.gocartacho.gocartacho.model.DiaSemana.Lunes, 14, com.gocartacho.gocartacho.model.NivelAfluencia.Medio);
            createAfluencia(zId, com.gocartacho.gocartacho.model.DiaSemana.Lunes, 20, com.gocartacho.gocartacho.model.NivelAfluencia.Bajo);
            
            // Viernes
            createAfluencia(zId, com.gocartacho.gocartacho.model.DiaSemana.Viernes, 14, com.gocartacho.gocartacho.model.NivelAfluencia.Medio);
            createAfluencia(zId, com.gocartacho.gocartacho.model.DiaSemana.Viernes, 20, com.gocartacho.gocartacho.model.NivelAfluencia.Alto);
            createAfluencia(zId, com.gocartacho.gocartacho.model.DiaSemana.Viernes, 22, com.gocartacho.gocartacho.model.NivelAfluencia.Alto);
            
            // Sábado
            createAfluencia(zId, com.gocartacho.gocartacho.model.DiaSemana.Sábado, 12, com.gocartacho.gocartacho.model.NivelAfluencia.Medio);
            createAfluencia(zId, com.gocartacho.gocartacho.model.DiaSemana.Sábado, 16, com.gocartacho.gocartacho.model.NivelAfluencia.Alto);
            createAfluencia(zId, com.gocartacho.gocartacho.model.DiaSemana.Sábado, 21, com.gocartacho.gocartacho.model.NivelAfluencia.Alto);
            
            // Domingo
            createAfluencia(zId, com.gocartacho.gocartacho.model.DiaSemana.Domingo, 10, com.gocartacho.gocartacho.model.NivelAfluencia.Bajo);
            createAfluencia(zId, com.gocartacho.gocartacho.model.DiaSemana.Domingo, 13, com.gocartacho.gocartacho.model.NivelAfluencia.Alto);
            createAfluencia(zId, com.gocartacho.gocartacho.model.DiaSemana.Domingo, 18, com.gocartacho.gocartacho.model.NivelAfluencia.Medio);
        }
        log.info("Afluencia histórica inicializada correctamente.");
    }

    private void createAfluencia(String zonaId, com.gocartacho.gocartacho.model.DiaSemana dia, int hora, com.gocartacho.gocartacho.model.NivelAfluencia nivel) {
        com.gocartacho.gocartacho.model.AfluenciaHistorica a = new com.gocartacho.gocartacho.model.AfluenciaHistorica();
        a.setZonaId(zonaId);
        a.setDiaSemana(dia);
        a.setHora(hora);
        a.setNivelPromedio(nivel);
        afluenciaHistoricaRepository.save(a);
    }
}
