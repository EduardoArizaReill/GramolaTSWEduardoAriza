/* CHECK 
 * Repositorio encargado de acceder a la tabla subscription_plan, sin esto lo es capaz de poner bien lo que cuenta
 * cada cancion.
 *
 * Esta clase me permite consultar los planes de pago activos,
 * tanto los planes de suscripción como el plan de pago por canción.
 */
package edu.uclm.esi.Gramola_Backend.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.esi.Gramola_Backend.model.SubscriptionPlan;

@Repository
public interface SubscriptionPlanDao extends JpaRepository<SubscriptionPlan, String> {

    /*
     * Busco todos los planes activos de un tipo concreto.
     * Por ejemplo, los planes de tipo SUBSCRIPTION para mostrar mensual y anual.
     * Los ordeno por sortOrder para controlar cómo aparecen en pantalla.
     */
    List<SubscriptionPlan> findByActiveTrueAndPlanTypeOrderBySortOrderAsc(String planType);

    /*
     * Busco el primer plan activo de un tipo concreto.
     * Esto lo uso especialmente para obtener el plan SONG, es decir,
     * el pago por canción.
     */
    Optional<SubscriptionPlan> findFirstByActiveTrueAndPlanTypeOrderBySortOrderAsc(String planType);
}