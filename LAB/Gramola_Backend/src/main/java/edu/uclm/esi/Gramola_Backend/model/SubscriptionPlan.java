/* CHECK
 * Modelo que representa un plan de pago de la aplicación.
 *
 * Esta clase se corresponde con la tabla subscription_plan.
 * La uso para guardar los precios y características de los planes,
 * como mensual, anual o pago por canción.
 *
 * Gracias a esta clase, los precios no están hardcodeados en el código,
 * sino guardados en base de datos.
 */
package edu.uclm.esi.Gramola_Backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscription_plan")
public class SubscriptionPlan {

    /*
     * Identificador del plan.
     * Ejemplos: monthly, yearly, song.
     */
    @Id
    private String id;

    /*
     * Nombre visible del plan.
     */
    private String name;

    /*
     * Descripción que se puede mostrar en el frontend.
     */
    private String description;

    /*
     * Precio en céntimos.
     * Por ejemplo, 888 significa 8,88 €.
     */
    @Column(name = "amount_cents")
    private int amountCents;

    /*
     * Moneda del pago.
     */
    private String currency;

    /*
     * Tipo de plan.
     * SUBSCRIPTION para mensual/anual.
     * SONG para pago por canción.
     */
    @Column(name = "plan_type")
    private String planType;

    /*
     * Intervalo del plan.
     * MONTH, YEAR u ONE_TIME.
     */
    @Column(name = "interval_type")
    private String intervalType;

    /*
     * Indica si este plan debe aparecer como recomendado.
     */
    private boolean recommended;

    /*
     * Indica si el plan está activo.
     */
    private boolean active = true;

    /*
     * Orden en el que se muestran los planes.
     */
    @Column(name = "sort_order")
    private int sortOrder = 0;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getAmountCents() { return amountCents; }
    public void setAmountCents(int amountCents) { this.amountCents = amountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public String getIntervalType() { return intervalType; }
    public void setIntervalType(String intervalType) { this.intervalType = intervalType; }

    public boolean isRecommended() { return recommended; }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}