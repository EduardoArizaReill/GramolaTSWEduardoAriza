/* CHECK
 * Modelo que representa un usuario o bar registrado en la aplicación.
 *
 * Esta clase se corresponde con la tabla user.
 * Guarda los datos principales del bar, como email, contraseña, nombre,
 * credenciales de Spotify, token de confirmación, token de recuperación,
 * estado de suscripción y precio por canción.
 *
 * Es una de las entidades más importantes porque muchas partes del proyecto
 * dependen del usuario: login, pagos, Spotify y canciones solicitadas.
 */
package edu.uclm.esi.Gramola_Backend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class User {

    /*
     * Email del usuario.
     * Lo uso como clave primaria.
     */
    @Id
    private String email;

    /*
     * Contraseña guardada como hash.
     * No guardo la contraseña real en texto plano.
     */
    private String pwd;

    /*
     * Token de confirmación de cuenta.
     * Se crea cuando el usuario se registra.
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "creation_token_id")
    private Token creationToken;

    /*
     * Token de recuperación de contraseña.
     * Solo se usa cuando el usuario solicita cambiar su contraseña.
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "reset_token_id")
    private Token resetToken;

    /*
     * Client ID de Spotify del bar.
     */
    @Column(name = "client_id")
    private String clientId;

    /*
     * Client Secret de Spotify del bar.
     */
    @Column(name = "client_secret")
    private String clientSecret;

    /*
     * Nombre del bar.
     */
    @Column(name = "bar_name", nullable = false, length = 120)
    private String barName;

    /*
     * Indica si el bar ya pagó la suscripción.
     */
    @Column(name = "subscription_paid")
    private boolean subscriptionPaid = false;

    /*
     * Precio personalizado que el bar cobra por canción, en céntimos.
     * Por defecto 88 significa 0,88 €.
     */
    @Column(name = "song_price_cents", nullable = false)
    private int songPriceCents = 88;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public Token getCreationToken() {
        return creationToken;
    }

    public void setCreationToken(Token creationToken) {
        this.creationToken = creationToken;
    }

    public Token getResetToken() {
        return resetToken;
    }

    public void setResetToken(Token resetToken) {
        this.resetToken = resetToken;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getBarName() {
        return barName;
    }

    public void setBarName(String barName) {
        this.barName = barName;
    }

    public boolean isSubscriptionPaid() {
        return subscriptionPaid;
    }

    public void setSubscriptionPaid(boolean subscriptionPaid) {
        this.subscriptionPaid = subscriptionPaid;
    }

    public int getSongPriceCents() {
        return songPriceCents;
    }

    public void setSongPriceCents(int songPriceCents) {
        this.songPriceCents = songPriceCents;
    }
}