/* CHECK
 * Servicio encargado de gestionar las URLs configurables de la aplicación.
 *
 * Esta clase lee las URLs desde la tabla app_url usando AppUrlDao.
 * La uso para no tener URLs hardcodeadas en el código, tanto en backend
 * como en frontend.
 *
 * También tiene un método auxiliar para unir URLs sin crear barras dobles.
 */
package edu.uclm.esi.Gramola_Backend.services;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.Gramola_Backend.dao.AppUrlDao;
import edu.uclm.esi.Gramola_Backend.model.AppUrl;

@Service
public class AppUrlService {

    /*
     * DAO que me permite consultar la tabla app_url.
     */
    private final AppUrlDao appUrlDao;

    /*
     * Inyecto AppUrlDao por constructor.
     * Así Spring me proporciona automáticamente el repositorio.
     */
    public AppUrlService(AppUrlDao appUrlDao) {
        this.appUrlDao = appUrlDao;
    }

    /*
     * Leo una URL desde base de datos usando su clave.
     * Si no existe o está desactivada, lanzo un error para detectarlo rápido.
     */
    public String getUrl(String id) {
        AppUrl appUrl = appUrlDao.findByIdAndActiveTrue(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No existe la URL activa en base de datos: " + id
                )
            );

        return appUrl.getUrlValue();
    }

    /*
     * Devuelvo todas las URLs activas en formato clave-valor.
     * Esto lo usa Angular para cargar las URLs desde el backend.
     */
    public Map<String, String> getAllActiveUrls() {
        return appUrlDao.findByActiveTrue()
            .stream()
            .collect(Collectors.toMap(
                AppUrl::getId,
                AppUrl::getUrlValue
            ));
    }

    /*
     * Uno una URL base con una parte final.
     * Lo hago así para evitar errores como barras dobles o falta de barra.
     */
    public String join(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return path;
        }

        if (path == null || path.isBlank()) {
            return baseUrl;
        }

        String cleanBase = baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1)
            : baseUrl;

        String cleanPath = path.startsWith("/")
            ? path
            : "/" + path;

        return cleanBase + cleanPath;
    }
}