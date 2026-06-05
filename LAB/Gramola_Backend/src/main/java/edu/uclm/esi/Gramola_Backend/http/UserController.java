/* CHECK
 * Controlador encargado de gestionar las operaciones relacionadas con usuarios.
 *
 * Desde aquí recibo las peticiones del frontend para registrar usuarios,
 * iniciar sesión, confirmar cuentas por correo, recuperar contraseña,
 * cambiar contraseña y borrar usuarios.
 *
 * Esta clase no guarda directamente en base de datos. Su función principal
 * es recibir la petición HTTP, validar los datos básicos y llamar a UserService,
 * que es quien contiene la lógica real de usuarios.
 */
package edu.uclm.esi.Gramola_Backend.http;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.Gramola_Backend.model.User;
import edu.uclm.esi.Gramola_Backend.services.AppUrlService;
import edu.uclm.esi.Gramola_Backend.services.UserService;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("users")
public class UserController {

    /*
     * Servicio principal de usuarios.
     * Lo uso para registrar, iniciar sesión, confirmar cuenta,
     * recuperar contraseña y borrar usuarios.
     */
    @Autowired
    private UserService service;

    /*
     * Servicio para obtener URLs desde la base de datos.
     * Lo uso, por ejemplo, para redirigir al usuario a la pantalla de pago
     * sin dejar la URL escrita directamente en el código.
     */
    @Autowired
    private AppUrlService appUrlService;

    /*
     * Endpoint para registrar un nuevo bar/usuario.
     *
     * El frontend envía un JSON y yo lo recibo como Map.
     * De ese Map saco el nombre del bar, email, contraseñas
     * y credenciales de Spotify.
     */
    @PostMapping("/register")
    public Map<String, String> register(@RequestBody Map<String, String> body) {
        String bar = body.get("bar");
        String email = body.get("email");
        String pwd1 = body.get("pwd1");
        String pwd2 = body.get("pwd2");
        String clientId = body.get("clientId");
        String clientSecret = body.get("clientSecret");

        /*
         * Compruebo que el nombre del bar no venga vacío.
         */
        if (bar == null || bar.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Bar name is required");
        }

        /*
         * Compruebo que el email tenga una forma básica válida.
         */
        if (email == null || !email.contains("@") || !email.contains(".")) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Email is not valid");
        }

        /*
         * Compruebo que las dos contraseñas existan y coincidan.
         */
        if (pwd1 == null || pwd2 == null || !pwd1.equals(pwd2)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Passwords do not match");
        }

        /*
         * Obligo a que la contraseña tenga al menos 8 caracteres.
         */
        if (pwd1.length() < 8) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Password must be at least 8 characters long");
        }

        /*
         * Compruebo que el bar haya introducido su Client ID de Spotify.
         */
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Client ID is required");
        }

        /*
         * Compruebo que el bar haya introducido su Client Secret de Spotify.
         */
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Client Secret is required");
        }

        /*
         * Si todas las validaciones son correctas, llamo al servicio.
         * El servicio se encarga de guardar el usuario, crear el token
         * y enviar el correo de confirmación.
         */
        String tokenId = this.service.register(
            bar.trim(),
            email.trim(),
            pwd1,
            clientId.trim(),
            clientSecret.trim()
        );

        /*
         * Devuelvo un mensaje al frontend junto con el token generado.
         */
        return Map.of(
            "message", "Usuario registrado correctamente",
            "tokenId", tokenId
        );
    }

    /*
     * Endpoint para iniciar sesión.
     *
     * El frontend envía email y contraseña. Yo llamo al servicio para validar
     * las credenciales y, si todo está bien, devuelvo los datos básicos del bar.
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String pwd = body.get("pwd");

        /*
         * UserService comprueba la contraseña, si la cuenta está confirmada
         * y si la suscripción está pagada.
         */
        User user = this.service.login(email, pwd);

        /*
         * Devuelvo al frontend solo los datos necesarios.
         * No devuelvo la contraseña ni datos sensibles.
         */
        return Map.of(
            "email", user.getEmail(),
            "barName", user.getBarName(),
            "clientId", user.getClientId(),
            "subscriptionPaid", user.isSubscriptionPaid()
        );
    }

    /*
     * Endpoint auxiliar para obtener el token de creación de un usuario.
     * Puede servir para pruebas o para recuperar el token asociado a un email.
     */
    @GetMapping("/token/{email}")
    public String getToken(@PathVariable String email) {
        return this.service.getTokenIdForUser(email);
    }

    /*
     * Endpoint para borrar un usuario por email.
     */
    @DeleteMapping("/delete")
    public void delete(@RequestParam String email) {
        this.service.delete(email);
    }

    /*
     * Endpoint que se ejecuta cuando el usuario pulsa el enlace del correo
     * de confirmación.
     *
     * Primero confirmo el token y después redirijo al usuario a la pantalla
     * de pago del frontend.
     */
    @GetMapping("/confirmToken/{email}")
    public void confirmToken(
        @PathVariable String email,
        @RequestParam String token,
        HttpServletResponse response
    ) throws IOException {

        /*
         * Confirmo que el token pertenece al usuario, que no está caducado
         * y que no se ha usado antes.
         */
        this.service.confirmToken(email, token);

        /*
         * Después de confirmar la cuenta, llevo al usuario a la pantalla de pago.
         * La URL sale de base de datos para no tenerla hardcodeada.
         */
        String paymentUrl = this.appUrlService.getUrl("FRONTEND_PAYMENT_URL");
        String redirectUrl = paymentUrl + "?token=" + encode(token);

        /*
         * Redirijo el navegador del usuario hacia Angular.
         */
        response.sendRedirect(redirectUrl);
    }

    /*
     * Endpoint para solicitar la recuperación de contraseña.
     *
     * El frontend manda el email y el servicio se encarga de crear un token
     * de recuperación y enviar el correo correspondiente.
     */
    @PostMapping("/forgotPassword")
    public Map<String, String> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        /*
         * Pido al servicio que gestione el correo de recuperación.
         * Si el email no existe, el servicio no revela esa información.
         */
        this.service.requestPasswordReset(email);

        return Map.of(
            "message",
            "Si el correo existe, recibirás un enlace para cambiar tu contraseña."
        );
    }

    /*
     * Endpoint para cambiar la contraseña.
     *
     * El frontend manda email, token y las dos nuevas contraseñas.
     * El servicio comprueba que el token sea válido antes de cambiarla.
     */
    @PostMapping("/resetPassword")
    public Map<String, String> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String token = body.get("token");
        String pwd1 = body.get("pwd1");
        String pwd2 = body.get("pwd2");

        /*
         * Delego en UserService toda la lógica de validación del token
         * y actualización de contraseña.
         */
        this.service.resetPassword(email, token, pwd1, pwd2);

        return Map.of(
            "message",
            "Contraseña actualizada."
        );
    }

    /*
     * Codifico valores para poder usarlos de forma segura dentro de una URL.
     * Lo uso especialmente con tokens o emails que pueden contener caracteres especiales.
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}