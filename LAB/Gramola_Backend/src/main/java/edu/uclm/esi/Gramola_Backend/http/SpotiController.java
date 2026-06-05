/* CHECK
 * Controlador encargado de gestionar la comunicación con Spotify desde el backend
 * Si lo quitas se acabo la información con spoti.
 *
 * Desde aquí el frontend puede conectar una cuenta de Spotify, intercambiar el code
 * por un token, consultar playlists, dispositivos, canción actual y buscar canciones.
 *
 * Este controlador no llama directamente a Spotify para todo, sino que delega la lógica
 * en SpotiService. Además, las URLs importantes de Spotify se obtienen desde base de datos
 * usando AppUrlService para evitar hardcodearlas.
 */
package edu.uclm.esi.Gramola_Backend.http;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.esi.Gramola_Backend.model.SpotiToken;
import edu.uclm.esi.Gramola_Backend.services.AppUrlService;
import edu.uclm.esi.Gramola_Backend.services.SpotiService;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping({ "/api/spoti", "/spoti" })
public class SpotiController {

    /*
     * Servicio que contiene la lógica de conexión y llamadas a Spotify.
     */
    private final SpotiService spotiService;

    /*
     * Servicio para obtener URLs desde base de datos.
     * Lo uso para obtener URLs como el callback y la autorización de Spotify.
     */
    private final AppUrlService appUrlService;

    /*
     * Inyecto los servicios por constructor.
     */
    public SpotiController(
        SpotiService spotiService,
        AppUrlService appUrlService
    ) {
        this.spotiService = spotiService;
        this.appUrlService = appUrlService;
    }

    /*
     * Intercambio el code que devuelve Spotify por un token de acceso.
     *
     * El frontend recibe un code en el callback y luego llama a este endpoint
     * para que el backend consiga el access token de Spotify.
     */
    @GetMapping("/getAuthorizationToken")
    public SpotiToken getAuthorizationToken(
        @RequestParam String code,
        @RequestParam String email
    ) {
        return spotiService.getAuthorizationToken(code, email);
    }

    /*
     * Inicia el proceso de conexión con Spotify.
     *
     * Este endpoint redirige al usuario a la pantalla oficial de Spotify para
     * autorizar permisos. Uso el email para obtener el clientId del bar.
     */
    @GetMapping("/connect")
    public void connect(
        @RequestParam String email,
        HttpServletResponse response
    ) throws IOException {

        /*
         * Obtengo el clientId de Spotify asociado al bar.
         */
        String clientId = this.spotiService.getClientIdForUser(email);

        /*
         * Cojo las URLs desde base de datos para no dejarlas fijas en el código.
         */
        String redirectUri = this.appUrlService.getUrl("SPOTIFY_CALLBACK_URL");
        String spotifyAuthorizeUrl = this.appUrlService.getUrl("SPOTIFY_AUTHORIZE_URL");

        /*
         * Permisos que solicito a Spotify.
         * Los necesito para leer datos del usuario, playlists, estado de reproducción
         * y modificar la cola/reproducción.
         */
        String scope =
            "user-read-private " +
            "user-read-email " +
            "playlist-read-private " +
            "user-read-playback-state " +
            "user-modify-playback-state";

        /*
         * Creo un state con el email y un UUID para mantener información
         * durante el flujo de autorización.
         */
        String state = email + "|" + UUID.randomUUID();

        /*
         * Construyo la URL final de autorización de Spotify.
         */
        String spotifyUrl =
            spotifyAuthorizeUrl +
            "?response_type=code" +
            "&client_id=" + encode(clientId) +
            "&scope=" + encode(scope) +
            "&redirect_uri=" + encode(redirectUri) +
            "&state=" + encode(state);

        /*
         * Redirijo al navegador del usuario a Spotify.
         */
        response.sendRedirect(spotifyUrl);
    }

    /*
     * Devuelvo las playlists del usuario conectado.
     *
     * El token llega en la cabecera Authorization.
     */
    @GetMapping("/me/playlists")
    public String getPlaylists(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        return this.spotiService.getPlaylists(token);
    }

    /*
     * Devuelvo los dispositivos disponibles de Spotify.
     *
     * Esto permite saber en qué dispositivo se puede reproducir o mandar canciones.
     */
    @GetMapping("/me/player/devices")
    public String getDevices(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        return this.spotiService.getDevices(token);
    }

    /*
     * Devuelvo la canción que está sonando actualmente en Spotify.
     */
    @GetMapping("/me/player/currently-playing")
    public String getCurrentlyPlaying(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        return this.spotiService.getCurrentlyPlaying(token);
    }

    /*
     * Busco canciones en Spotify usando un texto introducido por el usuario.
     */
    @GetMapping("/search")
    public String searchTracks(
        @RequestParam String query,
        @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        return this.spotiService.searchTracks(token, query);
    }

    /*
     * Extraigo el token quitando la palabra Bearer de la cabecera Authorization.
     */
    private String extractToken(String authHeader) {
        return authHeader.replace("Bearer ", "");
    }

    /*
     * Codifico valores para poder meterlos en una URL sin problemas.
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}