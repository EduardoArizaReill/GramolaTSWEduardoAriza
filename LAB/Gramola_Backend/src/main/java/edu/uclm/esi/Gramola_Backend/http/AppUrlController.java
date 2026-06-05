/* CHECK
 * Controlador encargado de exponer al frontend las URLs guardadas en base de datos.
 *
 * Esta clase permite que Angular pueda pedir las URLs activas de la tabla app_url.
 * Gracias a esto, el frontend no necesita tener hardcodeadas URLs como payments,
 * users, Spotify o requestedSong.
 *
 * No accedo directamente a la base de datos desde aquí, sino que llamo a AppUrlService,
 * que es quien contiene la lógica para recuperar las URLs activas.
 */
package edu.uclm.esi.Gramola_Backend.http;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.esi.Gramola_Backend.services.AppUrlService;

@RestController
@RequestMapping("appUrls")
public class AppUrlController {
    /*
     * Servicio que uso para obtener URLs desde la base de datos.
     */
    private final AppUrlService appUrlService;

    public AppUrlController(AppUrlService appUrlService) {
        this.appUrlService = appUrlService;
    }

    /*
     * Devuelvo todas las URLs activas.
     * El frontend podrá cargarlas al arrancar y dejar de tenerlas hardcodeadas.
     */
    @GetMapping
    public Map<String, String> getAllActiveUrls() {
        return this.appUrlService.getAllActiveUrls();
    }

    /*
     * Devuelvo una URL concreta por su clave.
     * Lo dejo por si en algún punto solo necesito una URL específica.
     */
    @GetMapping("/{id}")
    public Map<String, String> getUrl(@PathVariable String id) {
        return Map.of(
            "id", id,
            "url", this.appUrlService.getUrl(id)
        );
    }
}