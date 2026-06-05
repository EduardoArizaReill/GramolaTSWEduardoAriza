/* CHECK
 * Modelo utilizado para recibir la respuesta de Spotify cuando se pide un token.
 *
 * Esta clase no representa una tabla de MySQL.
 * Sirve para mapear la respuesta JSON que devuelve Spotify al intercambiar
 * el code de autorización por un access token.
 *
 * Con este token el frontend/backend puede hacer llamadas a Spotify,
 * como buscar canciones, ver dispositivos o consultar playlists.
 */
package edu.uclm.esi.Gramola_Backend.model;

public class SpotiToken {

    /*
     * Token de acceso que devuelve Spotify.
     */
    private String access_token;

    /*
     * Tipo de token. Normalmente será Bearer.
     */
    private String token_type;

    /*
     * Tiempo de validez del token en segundos.
     */
    private int expires_in;

    /*
     * Token para renovar el access token si Spotify lo devuelve.
     */
    private String refresh_token;

    /*
     * Permisos concedidos por Spotify.
     */
    private String scope;

    public SpotiToken() {
    }

    public String getAccess_token() {
        return access_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public int getExpires_in() {
        return expires_in;
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public String getScope() {
        return scope;
    }
}