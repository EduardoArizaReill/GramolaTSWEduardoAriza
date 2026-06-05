/* CHECK
 * Modelo que representa una canción solicitada y pagada por un cliente.
 *
 * Esta clase se corresponde con la tabla requested_song.
 * La uso para guardar las canciones que forman parte de la cola del bar.
 *
 * Una canción entra con played = false, es decir, pendiente.
 * Cuando Spotify acepta la canción, se marca como played = true.
 */
package edu.uclm.esi.Gramola_Backend.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "requested_song")
public class RequestedSong {

    /*
     * Identificador único de la canción solicitada.
     * Lo genero automáticamente con UUID.
     */
    @Id
    @Column(length = 255)
    private String id;

    /*
     * Título de la canción.
     */
    @Column(length = 255, nullable = false)
    private String title;

    /*
     * Artista principal de la canción.
     */
    @Column(length = 255, nullable = false)
    private String artist;

    /*
     * URI de Spotify.
     * Es lo que luego se usa para enviar la canción a la cola de Spotify.
     */
    @Column(length = 255, nullable = false)
    private String uri;

    /*
     * Imagen de portada de la canción o álbum.
     */
    @Column(length = 255)
    private String image;

    /*
     * Fecha en milisegundos.
     * La uso para ordenar las canciones según el momento en el que se solicitaron.
     */
    @Column(name = "date", nullable = false)
    private long date;

    /*
     * Indica si la canción ya fue enviada/usada.
     * false significa pendiente.
     * true significa ya reproducida o enviada a Spotify.
     */
    @Column(nullable = false)
    private boolean played = false;

    /*
     * Email del bar al que pertenece esta canción.
     */
    @Column(name = "bar_email", length = 255, nullable = false)
    private String barEmail;

    /*
     * Al crear una canción nueva genero su id, fecha actual
     * y la dejo como pendiente.
     */
    public RequestedSong() {
        this.id = UUID.randomUUID().toString();
        this.date = System.currentTimeMillis();
        this.played = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public boolean isPlayed() { return played; }
    public void setPlayed(boolean played) { this.played = played; }

    public String getBarEmail() { return barEmail; }
    public void setBarEmail(String barEmail) { this.barEmail = barEmail; }
}