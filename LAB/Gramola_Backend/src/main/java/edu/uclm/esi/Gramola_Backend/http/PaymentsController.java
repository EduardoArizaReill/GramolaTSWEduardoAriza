/* CHECK
 * Controlador encargado de gestionar las peticiones relacionadas con pagos
 * Sin esta clase no haypagos que se puedan hacer.
 *
 * Desde aquí el frontend puede pedir los planes de suscripción, consultar el precio
 * de una canción, preparar un pago con Stripe y confirmar una transacción.
 *
 * Esta clase no calcula precios ni habla directamente con Stripe. Para eso llama
 * a PaymentService, que contiene la lógica real de pagos.
 */
package edu.uclm.esi.Gramola_Backend.http;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.exception.StripeException;

import edu.uclm.esi.Gramola_Backend.model.StripeTransaction;
import edu.uclm.esi.Gramola_Backend.services.PaymentService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("payments")
public class PaymentsController {

    /*
     * Servicio de pagos.
     * Lo uso para consultar planes, preparar pagos y confirmar transacciones.
     */
    @Autowired
    private PaymentService service;

    /*
     * Devuelvo los planes de suscripción activos.
     *
     * Normalmente aquí se devuelven los planes mensual y anual,
     * que están guardados en la tabla subscription_plan.
     */
    @GetMapping("/plans")
    public List<Map<String, Object>> getPlans() {
        return this.service.getPlans();
    }

    /*
     * Devuelvo el plan de pago por canción.
     *
     * Si recibo el email del bar, el servicio puede devolver el precio personalizado
     * que ese bar cobra por cada canción.
     */
    @GetMapping("/songPlan")
    public Map<String, Object> getSongPlan(
        @RequestParam(name = "email", required = false) String email
    ) {
        return this.service.getSongPlan(email);
    }

    /*
     * Preparo un pago con Stripe.
     *
     * El frontend me manda qué plan quiere pagar. Puede ser mensual, anual o canción.
     * También puedo recibir el token del usuario cuando se está pagando una suscripción
     * después de confirmar la cuenta por correo.
     */
    @GetMapping("/prepay")
    public StripeTransaction prepay(
        @RequestParam(name = "plan") String plan,
        @RequestParam(name = "token", required = false) String token,
        @RequestParam(name = "email", required = false) String email,
        @RequestParam(name = "songPriceCents", required = false) Integer songPriceCents,
        HttpSession session
    ) {
        try {
            /*
             * Llamo al servicio para crear la transacción en Stripe.
             * El servicio devuelve un StripeTransaction con los datos necesarios
             * para que el frontend pueda continuar el pago.
             */
            StripeTransaction transactionDetails = this.service.prepay(plan, token, email);

            /*
             * Guardo los datos de la operación en sesión.
             * Luego los comparo cuando Stripe confirme el pago para asegurarme
             * de que se confirma la misma transacción que se preparó.
             */
            session.setAttribute("transactionDetails", transactionDetails);
            session.setAttribute("userToken", token);

            /*
             * Si el bar está pagando una suscripción y ha elegido un precio
             * por canción, lo guardo temporalmente en sesión para usarlo al confirmar.
             */
            if (songPriceCents != null) {
                session.setAttribute("songPriceCents", songPriceCents);
            }

            return transactionDetails;

        } catch (StripeException e) {
            /*
             * Si Stripe devuelve un error, lo transformo en un error HTTP entendible
             * para el frontend.
             */
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /*
     * Confirmo un pago después de que Stripe haya aceptado la tarjeta.
     *
     * Aquí compruebo que los datos recibidos coincidan con la transacción que yo
     * había guardado previamente en sesión.
     */
    @PostMapping("/confirm")
    public void confirm(HttpSession session, @RequestBody Map<String, Object> finalData) {

        /*
         * Recupero de sesión la transacción que había preparado antes.
         */
        StripeTransaction transactionDetails =
            (StripeTransaction) session.getAttribute("transactionDetails");

        if (transactionDetails == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay transacción en sesión.");
        }

        /*
         * La transacción guarda datos de Stripe en JSON.
         * Los leo para comparar el client_secret enviado inicialmente con el recibido.
         */
        JSONObject jso = new JSONObject(transactionDetails.getData());

        String sentTransactionId = transactionDetails.getId();
        String sentClientSecret = jso.getString("client_secret");

        /*
         * Datos finales que manda el frontend después de confirmar el pago con Stripe.
         */
        JSONObject jsoFinalData = new JSONObject(finalData);

        String userToken = jsoFinalData.optString("token", "");

        /*
         * Si el token no viene en el body, intento recuperarlo de sesión.
         */
        if (userToken == null || userToken.isBlank()) {
            Object sessionToken = session.getAttribute("userToken");

            if (sessionToken instanceof String) {
                userToken = (String) sessionToken;
            }
        }

        /*
         * Recupero el precio por canción que se guardó temporalmente en sesión.
         */
        Integer songPriceCents = null;
        Object sessionSongPrice = session.getAttribute("songPriceCents");

        if (sessionSongPrice instanceof Integer) {
            songPriceCents = (Integer) sessionSongPrice;
        }

        /*
         * Extraigo los datos que devuelve Stripe tras confirmar el pago.
         */
        String receivedTransactionId = jsoFinalData.getString("transactionId");
        String receivedClientSecret = jsoFinalData
            .getJSONObject("paymentIntent")
            .getString("client_secret");

        /*
         * Compruebo que la confirmación recibida coincide con la transacción
         * que el backend había preparado antes.
         */
        if (
            sentTransactionId.equals(receivedTransactionId)
            && sentClientSecret.equals(receivedClientSecret)
        ) {
            /*
             * Si todo coincide, confirmo la transacción en el servicio.
             * Ahí se activa la suscripción si era un pago mensual/anual.
             */
            this.service.confirmTransaction(transactionDetails, userToken, songPriceCents);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de confirmación no válidos.");
        }

        /*
         * Limpio la sesión para no dejar datos de pagos antiguos.
         */
        session.removeAttribute("transactionDetails");
        session.removeAttribute("userToken");
        session.removeAttribute("songPriceCents");
    }
}