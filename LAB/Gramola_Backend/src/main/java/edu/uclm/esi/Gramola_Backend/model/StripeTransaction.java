/* CHECK
 * Modelo que representa una transacción de Stripe.
 *
 * Esta clase se guarda en base de datos para registrar los pagos preparados
 * por el backend. Guardo el id de la transacción, el JSON devuelto por Stripe,
 * el email del usuario si se conoce y el plan que se está pagando.
 *
 * Me sirve para comparar después que la confirmación del pago corresponde
 * con la transacción que el backend preparó inicialmente.
 */
package edu.uclm.esi.Gramola_Backend.model;

import org.json.JSONObject;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.Map;
import java.util.UUID;

@Entity
public class StripeTransaction {

    /*
     * Identificador único de la transacción.
     */
    @Id
    @Column(length = 36)
    private String id;

    /*
     * JSON con los datos que devuelve Stripe.
     * Aquí se guarda, por ejemplo, el client_secret del PaymentIntent.
     */
    @Column(columnDefinition = "json")
    private String data;

    /*
     * Email del usuario asociado a la transacción, si se conoce.
     */
    private String email;

    /*
     * Plan que se pagó.
     * Puede ser monthly, yearly o song.
     */
    @Column(name = "plan_id", length = 50)
    private String planId;

    /*
     * Al crear una transacción genero automáticamente un UUID.
     */
    public StripeTransaction() {
        this.id = UUID.randomUUID().toString();
    }

    /*
     * Guardo los datos de Stripe como texto JSON.
     */
    public void setData(JSONObject transactionDetails) {
        this.data = transactionDetails.toString();
    }

    /*
     * Devuelvo los datos de Stripe como Map para poder acceder cómodamente
     * a sus campos desde el backend.
     */
    public Map<String, Object> getData() {
        return new JSONObject(this.data).toMap();
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }
}