/* CHECK
 * Modelo que representa la tabla app_url.
 *
 * Esta clase guarda las URLs configurables de la aplicación.
 * La uso para evitar tener URLs escritas directamente en el código.
 *
 * Cada registro tiene una clave identificadora, el valor de la URL
 * y un campo active para saber si esa URL se puede usar o no.
 */
package edu.uclm.esi.Gramola_Backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_url")
public class AppUrl {

    /*
     * Identificador de la URL.
     * Por ejemplo: PAYMENTS_BASE_URL, USERS_BASE_URL o SPOTIFY_SEARCH_URL.
     */
    @Id
    @Column(length = 100)
    private String id;

    /*
     * Valor real de la URL guardada en base de datos.
     */
    @Column(name = "url_value", nullable = false, length = 1000)
    private String urlValue;

    /*
     * Indica si la URL está activa.
     * Si está en false, no debería usarse.
     */
    @Column(nullable = false)
    private boolean active = true;

    public AppUrl() {
    }

    public String getId() {
        return id;
    }

    public String getUrlValue() {
        return urlValue;
    }

    public boolean isActive() {
        return active;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUrlValue(String urlValue) {
        this.urlValue = urlValue;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}