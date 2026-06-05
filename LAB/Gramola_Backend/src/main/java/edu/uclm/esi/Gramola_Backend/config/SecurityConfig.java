/* CHECK
 * Clase de configuración de seguridad del backend.
 *
 * Aquí configuro Spring Security para permitir las peticiones principales
 * de la aplicación, como registro, login y preparación de pagos.
 *
 * También configuro CORS para que el frontend de Angular pueda comunicarse
 * con el backend. Los orígenes permitidos no los dejo escritos directamente
 * en el código, sino que los cargo desde la base de datos usando AppUrlService.
 */
package edu.uclm.esi.Gramola_Backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import edu.uclm.esi.Gramola_Backend.services.AppUrlService;

@Configuration
public class SecurityConfig {

    /*
     * Uso este servicio para leer desde base de datos las URLs permitidas.
     * Así evito tener localhost o 127.0.0.1 escritos directamente en el código.
     */
    private final AppUrlService appUrlService;

    /*
     * Inyecto AppUrlService por constructor.
     * De esta forma Spring me lo proporciona automáticamente cuando arranca.
     */
    public SecurityConfig(AppUrlService appUrlService) {
        this.appUrlService = appUrlService;
    }

    /*
     * En este método configuro las reglas principales de seguridad.
     * Aquí indico qué endpoints dejo públicos y desactivo CSRF para trabajar
     * con una API REST llamada desde Angular.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                /*
                 * Dejo públicos estos endpoints porque el usuario todavía
                 * no tiene sesión cuando se registra, inicia sesión o prepara un pago.
                 */
                .requestMatchers(HttpMethod.POST, "/users/register", "/users/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/payments/prepay").permitAll()

                /*
                 * De momento permito el resto de peticiones.
                 * En este proyecto nos interesa que Angular pueda llamar a los endpoints
                 * sin tener una autenticación compleja con sesiones o JWT.
                 */
                .anyRequest().permitAll()
            );

        /*
         * Devuelvo la configuración final para que Spring Security la aplique.
         */
        return http.build();
    }

    /*
     * Aquí configuro CORS.
     * CORS es lo que permite que Angular, que se ejecuta en el puerto 4200,
     * pueda llamar al backend, que se ejecuta en el puerto 8080.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        /*
         * Cargo los orígenes permitidos desde base de datos.
         * Así cumplo con la idea de no dejar URLs hardcodeadas en el código.
         */
        configuration.setAllowedOrigins(List.of(
            appUrlService.getUrl("FRONTEND_LOCALHOST_URL"),
            appUrlService.getUrl("FRONTEND_127_URL")
        ));

        /*
         * Indico qué métodos HTTP permito desde el frontend.
         * GET para consultar, POST para crear/enviar datos, PUT para actualizar,
         * DELETE para borrar y OPTIONS para las comprobaciones previas de CORS.
         */
        configuration.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "OPTIONS"
        ));

        /*
         * Permito cualquier cabecera porque Angular puede mandar Content-Type,
         * Authorization u otras cabeceras según la petición.
         */
        configuration.setAllowedHeaders(List.of("*"));

        /*
         * Activo las credenciales para permitir cookies o sesión cuando hagan falta,
         * por ejemplo en el flujo de pagos donde se usa HttpSession.
         */
        configuration.setAllowCredentials(true);

        /*
         * Aplico esta configuración CORS a todos los endpoints del backend.
         */
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}