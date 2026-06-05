/*CHEK
 * Repositorio encargado de acceder a la tabla stripe_transaction, sin esta clase cualquier transacción no se gualda.
 * 
 * Esta clase me permite guardar y consultar las transacciones creadas con Stripe.
 * La uso para registrar los pagos preparados por el backend.
 */
package edu.uclm.esi.Gramola_Backend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.esi.Gramola_Backend.model.StripeTransaction;

@Repository
public interface StripeTransactionDao extends JpaRepository<StripeTransaction, String> {

    /*
     * No necesito métodos personalizados por ahora.
     * JpaRepository ya me da operaciones básicas como save, findById, findAll y delete.
     */
}