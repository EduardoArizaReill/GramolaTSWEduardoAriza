/* CHECK
 * Controlador encargado de gestionar la cola de canciones solicitadas, 
 * sin esto no funcionan ni la información que se muestra ni se guardan en cola las cosas 
 * hay que tener especial cuidado ya que es puente a RequestSongDao.
 * 
 * Desde aquí el frontend puede guardar una canción pagada, consultar la cola
 * pendiente de un bar, ver el historial de canciones, pedir la siguiente canción
 * pendiente y marcar una canción como usada después de enviarla a Spotify.
 *
 * Esta clase ayuda a cumplir el requisito de que el backend mantenga la lista
 * de canciones que componen la cola de reproducción.
 */
package edu.uclm.esi.Gramola_Backend.http;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.esi.Gramola_Backend.model.RequestedSong;
import edu.uclm.esi.Gramola_Backend.services.RequestedSongService;

@RestController
@RequestMapping("requestedSong")
public class RequestedSongController {

    /*
     * Servicio que contiene la lógica de canciones solicitadas.
     */
    @Autowired
    private RequestedSongService service;

    /*
     * Creo una nueva canción solicitada.
     *
     * Este endpoint se usa después de que una canción se haya pagado correctamente.
     * El frontend envía título, artista, uri de Spotify, imagen, fecha y email del bar.
     */
    @PostMapping
    public RequestedSong create(@RequestBody Map<String, Object> body) {
        return service.create(body);
    }

    /*
     * Devuelvo la cola pendiente de un bar.
     *
     * Solo devuelve canciones que todavía no han sido marcadas como reproducidas,
     * es decir, canciones con played = false.
     */
    @GetMapping("/queue/{barEmail}")
    public List<RequestedSong> getPendingQueue(@PathVariable String barEmail) {
        return service.getPendingQueue(barEmail);
    }

    /*
     * Devuelvo el historial completo de canciones de un bar.
     *
     * Aquí pueden aparecer tanto canciones pendientes como canciones ya enviadas
     * o usadas.
     */
    @GetMapping("/history/{barEmail}")
    public List<RequestedSong> getAllSongsFromBar(@PathVariable String barEmail) {
        return service.getAllSongsFromBar(barEmail);
    }

    /*
     * Devuelvo la siguiente canción pendiente sin marcarla como reproducida.
     *
     * Esto es importante porque solo debo marcarla como usada cuando Spotify
     * acepta realmente la petición.
     */
    @GetMapping("/queue/{barEmail}/next")
    public RequestedSong peekNextSong(@PathVariable String barEmail) {
        return service.peekNextSong(barEmail);
    }

    /*
     * Marco una canción como reproducida después de enviarla a Spotify.
     *
     * Este endpoint se llama cuando Spotify acepta añadir la canción a su cola.
     */
    @PostMapping("/{songId}/played")
    public RequestedSong markAsPlayed(@PathVariable String songId) {
        return service.markAsPlayed(songId);
    }

    /*
     * Devuelvo el número de canciones pendientes en la cola de un bar.
     *
     * Lo devuelvo dentro de un Map para responder en formato JSON.
     */
    @GetMapping("/queue/{barEmail}/size")
    public Map<String, Integer> getPendingQueueSize(@PathVariable String barEmail) {
        return Map.of("size", service.getPendingQueueSize(barEmail));
    }
}