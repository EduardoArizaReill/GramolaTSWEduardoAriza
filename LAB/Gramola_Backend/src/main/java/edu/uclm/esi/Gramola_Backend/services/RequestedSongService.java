/* CHECK
 * Servicio encargado de gestionar las canciones solicitadas por los usuarios
 * sin esto todo lo que tenga que ver con la música a la mierda.
 *
 * Esta clase contiene la lógica de la cola de canciones del backend.
 * Permite guardar canciones pagadas, consultar canciones pendientes,
 * consultar historial, obtener la siguiente canción y marcarla como reproducida.
 *
 * La cola se basa en la tabla requested_song:
 * played = false significa pendiente.
 * played = true significa ya enviada/usada.
 */
package edu.uclm.esi.Gramola_Backend.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.Gramola_Backend.dao.RequestedSongDao;
import edu.uclm.esi.Gramola_Backend.dao.UserDao;
import edu.uclm.esi.Gramola_Backend.model.RequestedSong;
import edu.uclm.esi.Gramola_Backend.model.User;

@Service
public class RequestedSongService {

    /*
     * DAO para guardar y consultar canciones solicitadas.
     */
    @Autowired
    private RequestedSongDao requestedSongDao;

    /*
     * DAO para comprobar que el bar existe y tiene suscripción pagada.
     */
    @Autowired
    private UserDao userDao;

    /*
     * Creo una canción solicitada después de que el pago se haya completado.
     */
    public RequestedSong create(Map<String, Object> body) {

        /*
         * Extraigo y valido los campos obligatorios de la canción.
         */
        String title = normalizeRequiredText(asString(body.get("title")), "title is required");
        String artist = normalizeRequiredText(asString(body.get("artist")), "artist is required");
        String uri = normalizeRequiredText(asString(body.get("uri")), "uri is required");

        String image = asString(body.get("image"));
        String barEmail = getBarEmailFromBody(body);
        Long date = asLong(body.get("date"));

        /*
         * Compruebo que el bar exista.
         */
        User bar = userDao.findById(barEmail).orElse(null);

        if (bar == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bar user not found");
        }

        /*
         * Compruebo que el bar tenga la suscripción pagada.
         */
        if (!bar.isSubscriptionPaid()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Bar subscription not paid");
        }

        /*
         * Creo el objeto RequestedSong y lo relleno con los datos recibidos.
         */
        RequestedSong requestedSong = new RequestedSong();
        requestedSong.setTitle(title);
        requestedSong.setArtist(artist);
        requestedSong.setUri(uri);
        requestedSong.setImage(hasText(image) ? image.trim() : null);
        requestedSong.setBarEmail(barEmail);
        requestedSong.setDate(date != null ? date : System.currentTimeMillis());

        /*
         * Cuando se guarda una canción pagada, entra pendiente.
         */
        requestedSong.setPlayed(false);

        return requestedSongDao.save(requestedSong);
    }

    /*
     * Devuelvo las canciones pendientes de un bar.
     */
    public List<RequestedSong> getPendingQueue(String barEmail) {
        String normalizedBarEmail = normalizeRequiredText(barEmail, "barEmail is required");

        return requestedSongDao.findByBarEmailAndPlayedFalseOrderByDateAsc(normalizedBarEmail);
    }

    /*
     * Devuelvo todas las canciones de un bar, tanto pendientes como ya usadas.
     */
    public List<RequestedSong> getAllSongsFromBar(String barEmail) {
        String normalizedBarEmail = normalizeRequiredText(barEmail, "barEmail is required");

        return requestedSongDao.findByBarEmailOrderByDateAsc(normalizedBarEmail);
    }

    /*
     * Devuelvo la siguiente canción pendiente, pero todavía no la marco como usada.
     * Solo se marca como usada cuando Spotify acepta la petición.
     */
    public RequestedSong peekNextSong(String barEmail) {
        String normalizedBarEmail = normalizeRequiredText(barEmail, "barEmail is required");

        return requestedSongDao.findFirstByBarEmailAndPlayedFalseOrderByDateAsc(normalizedBarEmail)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No hay canciones pendientes en la cola"
                )
            );
    }

    /*
     * Marco la canción como usada solo después de que Spotify acepte la petición.
     */
    @Transactional
    public RequestedSong markAsPlayed(String songId) {
        String normalizedSongId = normalizeRequiredText(songId, "songId is required");

        RequestedSong song = requestedSongDao.findById(normalizedSongId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Canción no encontrada"
                )
            );

        song.setPlayed(true);

        return requestedSongDao.save(song);
    }

    /*
     * Devuelvo cuántas canciones pendientes tiene un bar.
     */
    public int getPendingQueueSize(String barEmail) {
        String normalizedBarEmail = normalizeRequiredText(barEmail, "barEmail is required");

        return requestedSongDao.findByBarEmailAndPlayedFalseOrderByDateAsc(normalizedBarEmail).size();
    }

    /*
     * Obtengo el email del bar desde el body.
     * Acepto barEmail y bar_email para evitar problemas si el frontend cambia el nombre.
     */
    private String getBarEmailFromBody(Map<String, Object> body) {
        String barEmail = asString(body.get("barEmail"));

        if (!hasText(barEmail)) {
            barEmail = asString(body.get("bar_email"));
        }

        return normalizeRequiredText(barEmail, "barEmail is required");
    }

    /*
     * Compruebo si un texto tiene contenido real.
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /*
     * Normalizo un texto obligatorio.
     * Si viene vacío, lanzo error.
     */
    @NonNull
    private String normalizeRequiredText(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }

        return value.trim();
    }

    /*
     * Convierto un objeto a String de forma segura.
     */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /*
     * Convierto un objeto a Long si es posible.
     */
    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}