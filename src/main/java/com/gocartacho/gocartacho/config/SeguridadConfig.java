package com.gocartacho.gocartacho.config;

import com.gocartacho.gocartacho.security.FiltroAutenticacionJwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración principal de Spring Security.
 * Define las reglas de acceso (CORS, CSRF, endpoints públicos y privados),
 * y registra el filtro JWT para la autenticación sin estado (Stateless).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SeguridadConfig {

    private static final String ROLE_ADMIN = "ADMIN";

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final FiltroAutenticacionJwt jwtAuthFilter;
    private final com.gocartacho.gocartacho.security.OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    public SeguridadConfig(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, FiltroAutenticacionJwt jwtAuthFilter, com.gocartacho.gocartacho.security.OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtAuthFilter = jwtAuthFilter;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);

        http
            .authenticationProvider(authProvider)
            .csrf(csrf -> csrf.disable())
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Páginas y recursos públicos
                .requestMatchers(
                    "/", "/mapa", "/planes", "/planes/**",
                    "/explorar", "/explorar/**",
                    "/login", "/registro", "/register",
                    "/privacidad", "/acerca", "/mi-negocio",
                    "/promociones", "/error",
                    "/forgot-password", "/reset-password",
                    "/oauth2/**", "/login/oauth2/**"
                ).permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()

                // API Pública
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/zonas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/zonas", "/api/v1/heatmap/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/planes/**", "/api/v1/planes-inteligentes/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/comercios/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/promociones/activas", "/api/v1/promociones/zona/**", "/api/v1/promociones/**").permitAll()
                .requestMatchers("/api/v1/dev/**").permitAll()

                // Endpoints Protegidos para ADMINISTRADORES
                .requestMatchers("/api/v1/admin/**").hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/admin/register").hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/v1/comercios/pendientes", "/api/v1/comercios/inactivos").hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/comercios/*/estado").hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/promociones/*/estado").hasRole(ROLE_ADMIN)

                // Endpoints Protegidos (Cualquier usuario logueado vía JWT)
                .requestMatchers(HttpMethod.POST, "/api/v1/comercios", "/api/v1/promociones", "/api/v1/resenas").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/resenas/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/promociones/**").authenticated()
                .requestMatchers("/api/v1/favoritos/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/auth/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").authenticated()

                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .successHandler(oAuth2AuthenticationSuccessHandler)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}