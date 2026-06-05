/* CHECK
 * Repositorio encargado de acceder a la tabla requested_song.
 *
 * Esta clase me permite consultar y guardar las canciones pagadas por los usuarios.
 * También me permite obtener la cola pendiente de reproducción de cada bar.
 */
package edu.uclm.esi.Gramola_Backend.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.esi.Gramola_Backend.model.RequestedSong;

@Repository
public interface RequestedSongDao extends JpaRepository<RequestedSong, String> {

    /*
     * Busco las canciones pendientes de un bar.
     * Solo devuelve las que todavía no se han marcado como reproducidas.
     * Las ordeno por fecha para respetar el orden en el que fueron pagadas.
     */
    List<RequestedSong> findByBarEmailAndPlayedFalseOrderByDateAsc(String barEmail);

    /*
     * Busco todas las canciones de un bar.
     * Esto sirve para mostrar el historial completo, tanto pendientes como ya usadas.
     */
    List<RequestedSong> findByBarEmailOrderByDateAsc(String barEmail);

    /*
     * Busco la primera canción pendiente de un bar.
     * Esta es la siguiente canción que el backend debe mandar a Spotify.
     */
    Optional<RequestedSong> findFirstByBarEmailAndPlayedFalseOrderByDateAsc(String barEmail);
}