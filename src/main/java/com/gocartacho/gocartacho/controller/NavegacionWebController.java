package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.model.Plan;
import com.gocartacho.gocartacho.model.Zona;
import com.gocartacho.gocartacho.service.ComercioService;
import com.gocartacho.gocartacho.service.PromocionService;
import com.gocartacho.gocartacho.service.PlanService;
import com.gocartacho.gocartacho.service.UsuarioService;
import com.gocartacho.gocartacho.service.ZonaService;
import com.gocartacho.gocartacho.service.TipoNegocioService;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Optional;

/**
 * Controlador para manejar las peticiones HTTP relacionadas con NavegacionWeb.
 */
@Controller
public class NavegacionWebController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NavegacionWebController.class);
    private static final String ATTR_ZONAS = "zonas";
    private static final String ATTR_PLANES = "planes";

    private final ZonaService zonaService;
    private final PlanService planService;
    private final UsuarioService usuarioService;
    private final ComercioService comercioService;
    private final PromocionService promocionService;
    private final TipoNegocioService tipoNegocioService;
    private final com.gocartacho.gocartacho.service.ResenaService resenaService;

    public NavegacionWebController(ZonaService zonaService, PlanService planService,
            UsuarioService usuarioService, ComercioService comercioService,
            PromocionService promocionService, TipoNegocioService tipoNegocioService,
            com.gocartacho.gocartacho.service.ResenaService resenaService) {
        this.zonaService = zonaService;
        this.planService = planService;
        this.usuarioService = usuarioService;
        this.comercioService = comercioService;
        this.promocionService = promocionService;
        this.tipoNegocioService = tipoNegocioService;
        this.resenaService = resenaService;
    }

    // 1. Plan raíz → redirige al mapa interactivo, preservando parámetros si los
    // hay
    @GetMapping("/")
    public String landingPage(jakarta.servlet.http.HttpServletRequest request) {
        String queryString = request.getQueryString();
        return "redirect:/mapa" + (queryString != null ? "?" + queryString : "");
    }

    // 2. Página del Mapa Interactivo
    @GetMapping("/mapa")
    public String mapa(Model model) {
        try {
            model.addAttribute(ATTR_ZONAS, zonaService.obtenerTodasLasZonas());
            model.addAttribute("tiposNegocio", tipoNegocioService.listarTodos());
        } catch (Exception e) {
            LOGGER.error("Error cargando zonas: {}", e.getMessage(), e);
            model.addAttribute(ATTR_ZONAS, Collections.emptyList());
        }
        // La vista 'index.html' ahora sirve para /mapa
        return "index";
    }

    // 3. Página de Listado de Planes
    @GetMapping("/planes")
    public String planes(Model model) {
        try {
            model.addAttribute(ATTR_PLANES, planService.obtenerTodasLasPlanes());
        } catch (Exception e) {
            LOGGER.error("Error cargando planes: {}", e.getMessage(), e);
            model.addAttribute(ATTR_PLANES, Collections.emptyList());
        }
        return ATTR_PLANES;
    }

    // 4. Página de Detalle de Plan (Nueva)
    @GetMapping("/planes/{planId}")
    public String detallePlan(@PathVariable String planId, Model model) {
        Optional<Plan> planOpt = planService.obtenerPlanPorId(planId);
        if (planOpt.isEmpty()) {
            return "redirect:/planes?error=notfound";
        }
        model.addAttribute("plan", planOpt.get());
        return "plan-detalle"; // Necesitarás crear una vista 'plan-detalle.html'
    }

    // 5. Página de Promociones (Sin cambios por ahora)
    @GetMapping("/promociones")
    public String promociones() {
        return "promociones";
    }

    // 6. Página de Login
    @GetMapping("/login")
    public String login(Model model, HttpServletRequest request, HttpServletResponse response) {
        SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request, response);
        if (savedRequest != null) {
            model.addAttribute("targetUrl", savedRequest.getRedirectUrl());
        }
        return "login";
    }

    // 7. Página de Registro (Renombrada para consistencia)
    @GetMapping("/registro")
    public String register() {
        return "register";
    }

    // 7.1 Página de Solicitar Recuperación de Contraseña
    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    // 7.2 Página de Restablecer Contraseña (recibe el token por URL)
    @GetMapping("/reset-password")
    public String resetPassword(@org.springframework.web.bind.annotation.RequestParam(required = false) String token,
            Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    // 7.5 Página de Exploración de Negocios por Zona
    @GetMapping("/explorar/{zonaNumero}")
    public String explorarZona(@PathVariable String zonaNumero, Model model) {
        try {
            // Intentar buscar por número entero primero, luego por ObjectId como fallback
            Zona zona = null;
            try {
                int numero = Integer.parseInt(zonaNumero);
                zona = zonaService.obtenerZonaPorNumero(numero);
            } catch (NumberFormatException e) {
                // Si no es un número, buscar por ObjectId (para compatibilidad futura)
                zona = zonaService.obtenerZonaPorId(zonaNumero);
            }
            model.addAttribute("zona", zona);
            model.addAttribute("zonaNumero", zonaNumero); // lo necesita explorar.js
            model.addAttribute("tiposNegocio", tipoNegocioService.listarTodos());
        } catch (Exception e) {
            LOGGER.error("Error cargando datos para exploración: {}", e.getMessage());
        }
        return "explorar";
    }

    // 8. Dashboard Admin (Modular)
    @GetMapping("/dashboard")
    public String dashboardRedirect() {
        return "redirect:/dashboard/resumen";
    }

    @GetMapping("/dashboard/resumen")
    public String adminResumen(Model model) {
        cargarEstadisticasBase(model);
        return "admin/resumen";
    }

    @GetMapping("/dashboard/negocios")
    public String adminNegocios(Model model) {
        model.addAttribute("tiposNegocio", tipoNegocioService.listarTodos());
        return "admin/negocios";
    }

    @GetMapping("/dashboard/promociones")
    public String adminPromociones(Model model) {
        return "admin/promociones";
    }

    @GetMapping("/dashboard/usuarios")
    public String adminUsuarios(Model model) {
        return "admin/usuarios";
    }

    @GetMapping("/dashboard/moderacion")
    public String adminModeracion(Model model) {
        return "admin/moderacion";
    }

    @GetMapping("/dashboard/auditoria")
    public String adminAuditoria(Model model) {
        return "admin/auditoria";
    }

    private void cargarEstadisticasBase(Model model) {
        try {
            model.addAttribute(ATTR_ZONAS, zonaService.obtenerTodasLasZonas());
        } catch (Exception e) {
            model.addAttribute(ATTR_ZONAS, Collections.emptyList());
        }

        try {
            model.addAttribute("totalUsuarios", usuarioService.contarUsuarios());
            model.addAttribute("totalComercios", comercioService.contarComercios());
            model.addAttribute("totalResenas", resenaService.contarResenas());

            var promos = promocionService.obtenerPromocionesActivas();
            model.addAttribute("totalPromociones", promos != null ? (long) promos.size() : 0L);
        } catch (Exception e) {
            LOGGER.error("Error cargando estadísticas: {}", e.getMessage());
            model.addAttribute("totalUsuarios", 0L);
            model.addAttribute("totalComercios", 0L);
            model.addAttribute("totalPromociones", 0L);
            model.addAttribute("totalResenas", 0L);
        }
    }

    // 9. Página de Información y Privacidad (Renombrada)
    @GetMapping("/acerca")
    public String privacidad() {
        return "privacidad";
    }

    // 10. Portal Mi Negocio
    @GetMapping("/mi-negocio")
    public String miNegocio(Model model) {
        try {
            model.addAttribute(ATTR_ZONAS, zonaService.obtenerTodasLasZonas());
            model.addAttribute("tiposNegocio", tipoNegocioService.listarTodos());
        } catch (Exception e) {
            LOGGER.error("Error cargando zonas para mi-negocio: {}", e.getMessage(), e);
            model.addAttribute(ATTR_ZONAS, Collections.emptyList());
        }
        return "mi-negocio";
    }

    // Redirección para el plan antigua de registro para no romper enlaces
    @GetMapping("/register")
    public String registerRedirect() {
        return "redirect:/registro";
    }
}
