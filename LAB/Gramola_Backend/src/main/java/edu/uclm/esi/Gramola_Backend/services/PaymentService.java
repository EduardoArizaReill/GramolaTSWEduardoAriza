/* CHECK
 * Servicio encargado de gestionar los pagos de la aplicación.
 *
 * Esta clase trabaja con Stripe para preparar pagos y confirmar transacciones.
 * También consulta los planes de pago desde la base de datos para evitar
 * tener precios hardcodeados.
 *
 * Se usa tanto para suscripciones mensuales/anuales como para el pago
 * individual por canción.
 */
package edu.uclm.esi.Gramola_Backend.services;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import edu.uclm.esi.Gramola_Backend.dao.StripeTransactionDao;
import edu.uclm.esi.Gramola_Backend.dao.SubscriptionPlanDao;
import edu.uclm.esi.Gramola_Backend.dao.UserDao;
import edu.uclm.esi.Gramola_Backend.model.StripeTransaction;
import edu.uclm.esi.Gramola_Backend.model.SubscriptionPlan;
import edu.uclm.esi.Gramola_Backend.model.User;

@Service
public class PaymentService {

    /*
     * Clave secreta de Stripe en modo pruebas.
     * Con esta clave el backend puede crear PaymentIntent.
     */
    static {
        Stripe.apiKey = "";
    }

    /*
     * DAO para guardar transacciones de Stripe en base de datos.
     */
    @Autowired
    private StripeTransactionDao dao;

    /*
     * DAO para buscar usuarios.
     * Lo uso para asociar pagos a usuarios y activar la suscripción.
     */
    @Autowired
    private UserDao userDao;

    /*
     * DAO para consultar los planes de pago guardados en MySQL.
     */
    @Autowired
    private SubscriptionPlanDao planDao;

    /*
     * Devuelvo los planes de suscripción activos.
     * Los precios salen de base de datos, no del código.
     */
    public List<Map<String, Object>> getPlans() {
        return planDao.findByActiveTrueAndPlanTypeOrderBySortOrderAsc("SUBSCRIPTION")
            .stream()
            .map(p -> Map.<String, Object>of(
                "id", p.getId(),
                "name", p.getName(),
                "description", p.getDescription(),
                "amount", p.getAmountCents(),
                "currency", p.getCurrency(),
                "recommended", p.isRecommended(),
                "intervalType", p.getIntervalType(),
                "planType", p.getPlanType()
            ))
            .toList();
    }

    /*
     * Devuelvo el plan de canción.
     * Si el bar tiene un precio personalizado, uso ese precio.
     */
    public Map<String, Object> getSongPlan(String email) {
        SubscriptionPlan plan = planDao.findFirstByActiveTrueAndPlanTypeOrderBySortOrderAsc("SONG")
            .orElseThrow(() -> new RuntimeException("No hay plan SONG activo en BD"));

        int amount = getSongAmountForUser(plan, email);

        return Map.<String, Object>of(
            "id", plan.getId(),
            "name", plan.getName(),
            "description", plan.getDescription(),
            "amount", amount,
            "currency", plan.getCurrency(),
            "recommended", plan.isRecommended(),
            "intervalType", plan.getIntervalType(),
            "planType", plan.getPlanType()
        );
    }

    /*
     * Preparo el pago con Stripe.
     * El importe se calcula desde la base de datos y, si es una canción,
     * también puede depender del precio configurado para ese bar.
     */
    public StripeTransaction prepay(String planId, String userToken, String email) throws StripeException {

        /*
         * Compruebo que venga un plan válido.
         */
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("Missing plan");
        }

        String normalizedPlanId = planId.trim();

        /*
         * Busco el plan en base de datos.
         */
        SubscriptionPlan plan = planDao.findById(normalizedPlanId)
            .orElseThrow(() -> new IllegalArgumentException("Plan no válido: " + normalizedPlanId));

        long amount = plan.getAmountCents();

        /*
         * Si el plan es de tipo canción, miro si el bar tiene precio personalizado.
         */
        if ("SONG".equalsIgnoreCase(plan.getPlanType())) {
            amount = getSongAmountForUser(plan, email);
        }

        /*
         * Creo los parámetros del pago que se enviarán a Stripe.
         */
        PaymentIntentCreateParams createParams =
            PaymentIntentCreateParams.builder()
                .setCurrency(plan.getCurrency())
                .setAmount(amount)
                .build();

        /*
         * Creo el PaymentIntent en Stripe.
         */
        PaymentIntent intent = PaymentIntent.create(createParams);

        /*
         * Guardo la transacción en mi base de datos.
         */
        StripeTransaction transaction = new StripeTransaction();
        transaction.setData(new JSONObject(intent.toJson()));
        transaction.setPlanId(plan.getId());

        /*
         * Intento asociar la transacción al usuario correspondiente.
         */
        assignUserToTransaction(transaction, userToken, email);

        dao.save(transaction);

        return transaction;
    }

    /*
     * Confirmo una transacción de suscripción.
     * Si era un pago por canción, no activo la suscripción.
     */
    public void confirmTransaction(
        StripeTransaction transactionDetails,
        String userToken,
        Integer songPriceCents
    ) {
        try {
            String planId = transactionDetails.getPlanId();

            if (planId == null || planId.isBlank()) {
                return;
            }

            /*
             * Busco el plan que se pagó.
             */
            SubscriptionPlan plan = planDao.findById(planId).orElse(null);

            /*
             * Si no hay plan o el plan era de canción, no activo suscripción.
             */
            if (plan == null || "SONG".equalsIgnoreCase(plan.getPlanType())) {
                return;
            }

            if (userToken == null || userToken.isBlank()) {
                return;
            }

            /*
             * Busco el usuario mediante su token de creación.
             */
            User user = userDao.findByCreationToken_Id(userToken.trim());

            if (user == null) {
                return;
            }

            /*
             * Marco la suscripción como pagada.
             */
            user.setSubscriptionPaid(true);

            /*
             * Si se eligió un precio por canción durante el pago,
             * lo guardo también en el usuario.
             */
            if (songPriceCents != null && songPriceCents > 0) {
                user.setSongPriceCents(songPriceCents);
            }

            userDao.save(user);

        } catch (Exception e) {
            System.err.println("Error confirmando suscripción: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * Obtengo el precio de canción que debe aplicarse.
     * Por defecto uso el del plan SONG, pero si el usuario tiene uno propio,
     * uso el precio del usuario.
     */
    private int getSongAmountForUser(SubscriptionPlan plan, String email) {
        int amount = plan.getAmountCents();

        if (email == null || email.isBlank()) {
            return amount;
        }

        User user = userDao.findById(email.trim()).orElse(null);

        if (user != null && user.getSongPriceCents() > 0) {
            amount = user.getSongPriceCents();
        }

        return amount;
    }

    /*
     * Asocio una transacción a un usuario.
     * Puede venir identificado por token durante la suscripción
     * o por email cuando es un pago por canción.
     */
    private void assignUserToTransaction(
        StripeTransaction transaction,
        String userToken,
        String email
    ) {
        if (userToken != null && !userToken.isBlank()) {
            User user = userDao.findByCreationToken_Id(userToken.trim());

            if (user != null) {
                transaction.setEmail(user.getEmail());
            }

            return;
        }

        if (email != null && !email.isBlank()) {
            User user = userDao.findById(email.trim()).orElse(null);

            if (user != null) {
                transaction.setEmail(user.getEmail());
            }
        }
    }
}