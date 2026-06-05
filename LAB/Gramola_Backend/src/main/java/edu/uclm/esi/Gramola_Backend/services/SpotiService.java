/* CHECK
 * Servicio encargado de gestionar la comunicación con Spotify.
 *
 * Esta clase contiene la lógica necesaria para conectar un bar con Spotify,
 * obtener el token de autorización, consultar playlists, dispositivos,
 * canción actual y buscar canciones.
 *
 * No uso un DAO de Spotify porque Spotify no es una tabla de MySQL,
 * sino una API externa. Los datos que necesito del bar, como clientId
 * y clientSecret, los saco desde UserDao.
 */
package edu.uclm.esi.Gramola_Backend.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.Gramola_Backend.dao.UserDao;
import edu.uclm.esi.Gramola_Backend.model.SpotiToken;
import edu.uclm.esi.Gramola_Backend.model.User;

@Service
public class SpotiService {

    /*
     * DAO de usuarios.
     * Lo uso para buscar el bar y sacar de ahí sus credenciales de Spotify.
     */
    private final UserDao userDao;

    /*
     * Servicio de URLs.
     * Lo uso para leer desde base de datos las URLs de Spotify,
     * como la URL de token, callback, playlists, búsqueda, etc.
     */
    private final AppUrlService appUrlService;

    /*
     * Inyecto los servicios por constructor.
     */
    public SpotiService(UserDao userDao, AppUrlService appUrlService) {
        this.userDao = userDao;
        this.appUrlService = appUrlService;
    }

    /*
     * Devuelvo el clientId de Spotify asociado a un usuario/bar.
     *
     * Este método se usa cuando el frontend quiere conectar con Spotify.
     * Primero busco el usuario por email y luego compruebo que tenga clientId.
     */
    public String getClientIdForUser(String email) {
        User user = findUserByEmail(email);

        String clientId = user.getClientId();

        if (clientId == null || clientId.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El usuario no tiene client_id de Spotify configurado"
            );
        }

        return clientId;
    }

    /*
     * Intercambio el code que devuelve Spotify por un access token.
     *
     * Cuando el usuario autoriza la aplicación en Spotify, Spotify devuelve
     * un code. Con ese code, el clientId y el clientSecret, pido a Spotify
     * un token de acceso.
     */
    public SpotiToken getAuthorizationToken(String code, String email) {
        User user = findUserByEmail(email);

        String clientId = user.getClientId();
        String clientSecret = user.getClientSecret();

        /*
         * Compruebo que el usuario tenga sus credenciales de Spotify configuradas.
         */
        if (clientId == null || clientId.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El usuario no tiene client_id de Spotify configurado"
            );
        }

        if (clientSecret == null || clientSecret.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El usuario no tiene client_secret de Spotify configurado"
            );
        }

        /*
         * Spotify exige enviar clientId y clientSecret codificados como Basic Auth.
         */
        String header = basicAuth(clientId, clientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", header);

        /*
         * Uso la redirect URI y la URL de token desde base de datos.
         * Así evito tener estas URLs escritas directamente en el código.
         */
        String redirectUri = this.appUrlService.getUrl("SPOTIFY_CALLBACK_URL");
        String tokenUrl = this.appUrlService.getUrl("SPOTIFY_TOKEN_URL");

        /*
         * Construyo el body que espera Spotify para intercambiar el code.
         */
        String body =
            "grant_type=authorization_code" +
            "&code=" + encode(code) +
            "&redirect_uri=" + encode(redirectUri);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
            /*
             * Hago la petición POST a Spotify.
             * Spring convierte la respuesta JSON en un objeto SpotiToken.
             */
            ResponseEntity<SpotiToken> response =
                restTemplate.postForEntity(tokenUrl, request, SpotiToken.class);

            return response.getBody();

        } catch (HttpClientErrorException e) {
            /*
             * Si Spotify rechaza la petición, muestro el error por consola
             * y devuelvo un error entendible al frontend.
             */
            System.err.println("Error obteniendo token Spotify: " + e.getResponseBodyAsString());

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Spotify rechazó el intercambio de token. Revisa client_id, client_secret y redirect URI."
            );
        }
    }

    /*
     * Busco un usuario por email.
     *
     * Centralizo esta búsqueda para no repetir validaciones en varios métodos.
     */
    private User findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Email del bar obligatorio para conectar con Spotify"
            );
        }

        return userDao.findById(email.trim())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe ningún bar con ese email"
                )
            );
    }

    /*
     * Creo la cabecera Basic Auth que necesita Spotify.
     *
     * Spotify pide clientId:clientSecret codificado en Base64.
     */
    private String basicAuth(String clientId, String clientSecret) {
        String pair = clientId + ":" + clientSecret;

        return "Basic " + Base64.getEncoder()
            .encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }

    /*
     * Codifico valores para que puedan viajar dentro de una URL
     * sin problemas con caracteres especiales.
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /*
     * Creo los headers de Spotify en un método aparte para no repetir código.
     *
     * Todas las llamadas a Spotify necesitan enviar el access token
     * en la cabecera Authorization.
     */
    private HttpEntity<String> createSpotifyEntity(String userToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + userToken);

        return new HttpEntity<>(headers);
    }

    /*
     * Centralizo las llamadas GET a Spotify.
     *
     * Así reutilizo la misma lógica para playlists, dispositivos,
     * canción actual y búsqueda.
     */
    private String getFromSpotify(String url, String userToken, String errorMessage) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                createSpotifyEntity(userToken),
                String.class
            );

            return response.getBody();

        } catch (Exception e) {
            /*
             * Si hay error, lo muestro por consola.
             * Devuelvo null para no romper directamente la aplicación.
             */
            System.err.println(errorMessage + ": " + e.getMessage());
            return null;
        }
    }

    /*
     * Obtengo las playlists del usuario conectado en Spotify.
     */
    public String getPlaylists(String userToken) {
        String url = this.appUrlService.getUrl("SPOTIFY_PLAYLISTS_URL");

        return getFromSpotify(
            url,
            userToken,
            "Error llamando a Spotify Playlists"
        );
    }

    /*
     * Obtengo los dispositivos disponibles del usuario en Spotify.
     *
     * Esto sirve para saber a qué dispositivo se puede mandar la canción.
     */
    public String getDevices(String userToken) {
        String url = this.appUrlService.getUrl("SPOTIFY_DEVICES_URL");

        return getFromSpotify(
            url,
            userToken,
            "Error Spotify Devices"
        );
    }

    /*
     * Obtengo la canción que está sonando actualmente.
     */
    public String getCurrentlyPlaying(String userToken) {
        String url = this.appUrlService.getUrl("SPOTIFY_CURRENTLY_PLAYING_URL");

        return getFromSpotify(
            url,
            userToken,
            "Error Spotify Playing"
        );
    }

    /*
     * Busco canciones en Spotify a partir de un texto.
     *
     * La URL base sale de base de datos y luego añado query,
     * tipo track y límite de resultados.
     */
    public String searchTracks(String userToken, String query) {
        String searchUrl = this.appUrlService.getUrl("SPOTIFY_SEARCH_URL");

        String url =
            searchUrl +
            "?q=" + encode(query) +
            "&type=track&limit=10";

        return getFromSpotify(
            url,
            userToken,
            "Error buscando canciones"
        );
    }
}