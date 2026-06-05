/* CHECK
 * Modelo que representa un token temporal.
 *
 * Esta clase se usa para confirmar cuentas y para recuperar contraseñas.
 * Cada token tiene un id único, una fecha de creación, una fecha de uso
 * y una fecha de expiración.
 *
 * Si useTime vale 0, significa que el token todavía no se ha usado.
 * Si expiresTime ya ha pasado, el token está caducado.
 */
package edu.uclm.esi.Gramola_Backend.model;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Token {

    /*
     * Identificador único del token.
     */
    @Id
    @Column(length = 36)
    private String id;

    /*
     * Momento en el que se creó el token.
     */
    private long creationTime;

    /*
     * Momento en el que se usó el token.
     * Si vale 0, significa que no se ha usado todavía.
     */
    private long useTime = 0;

    /*
     * Momento en el que caduca el token.
     */
    private long expiresTime = 0;

    /*
     * Cuando creo un token nuevo, genero un UUID,
     * guardo la hora actual y marco una caducidad de 30 minutos.
     */
    public Token() {
        long now = System.currentTimeMillis();
        this.id = UUID.randomUUID().toString();
        this.creationTime = now;
        this.expiresTime = now + 1800000;
    }

    /*
     * Marco el token como usado guardando la hora actual.
     */
    public void use() {
        this.useTime = System.currentTimeMillis();
    }

    /*
     * Compruebo si el token ya fue usado.
     */
    public boolean isUsed() {
        return this.useTime != 0;
    }

    /*
     * Compruebo si el token ha caducado.
     */
    public boolean isExpired() {
        return this.expiresTime != 0 && System.currentTimeMillis() > this.expiresTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getUseTime() {
        return useTime;
    }

    public void setUseTime(long useTime) {
        this.useTime = useTime;
    }

    public long getExpiresTime() {
        return expiresTime;
    }

    public void setExpiresTime(long expiresTime) {
        this.expiresTime = expiresTime;
    }
}