/* CHECK
 * Servicio encargado de gestionar toda la lógica de usuarios.
 *
 * Esta clase se ocupa del registro, login, confirmación de cuenta,
 * recuperación de contraseña, cambio de contraseña y borrado de usuarios.
 *
 * También se encarga de crear tokens, enviar correos y comprobar si el usuario
 * está confirmado y tiene la suscripción pagada.
 */
package edu.uclm.esi.Gramola_Backend.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.Gramola_Backend.dao.UserDao;
import edu.uclm.esi.Gramola_Backend.model.Token;
import edu.uclm.esi.Gramola_Backend.model.User;
import edu.uclm.esi.Gramola_Backend.util.PasswordUtil;

@Service
public class UserService {

    /*
     * Servicio para enviar correos.
     * Lo uso en el registro y en la recuperación de contraseña.
     */
    @Autowired
    private EmailService emailService;

    /*
     * DAO de usuarios.
     * Lo uso para guardar, buscar y borrar usuarios en MySQL.
     */
    @Autowired
    private UserDao userDao;

    /*
     * Servicio de URLs.
     * Lo uso para construir enlaces de confirmación y recuperación
     * usando URLs guardadas en base de datos.
     */
    @Autowired
    private AppUrlService appUrlService;

    /*
     * Mapa antiguo de usuarios en memoria.
     * Actualmente la información real está en MySQL mediante UserDao.
     */
    private Map<String, User> users = new HashMap<>();

    /*
     * Registro un nuevo usuario/bar.
     *
     * Aquí creo el usuario, genero el token de confirmación,
     * guardo el usuario en MySQL y envío el correo de confirmación.
     */
    public String register(String bar, String email, String pwd, String clientId, String clientSecret) {

        /*
         * Primero compruebo si ya existe un usuario con ese email.
         */
        Optional<User> optUser = this.userDao.findById(email);

        if (optUser.isPresent()) {
            User existing = optUser.get();

            boolean confirmed = existing.getCreationToken() != null && existing.getCreationToken().isUsed();
            boolean paid = existing.isSubscriptionPaid();

            /*
             * Si el usuario existe pero no terminó el proceso,
             * lo borro para permitir que se registre de nuevo desde cero.
             */
            if (!confirmed || !paid) {
                this.userDao.delete(existing);
            } else {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El usuario ya existe y tiene suscripción activa"
                );
            }
        }

        /*
         * Creo un nuevo usuario con los datos recibidos.
         */
        User user = new User();
        user.setEmail(email);

        /*
         * Guardo la contraseña hasheada, no la contraseña real.
         */
        user.setPwd(PasswordUtil.hash(pwd));

        user.setBarName(bar);
        user.setSubscriptionPaid(false);
        user.setClientId(clientId);
        user.setClientSecret(clientSecret);

        /*
         * Creo un token de confirmación para este usuario.
         */
        user.setCreationToken(new Token());

        /*
         * Guardo el usuario en base de datos.
         */
        this.userDao.save(user);

        Token token = user.getCreationToken();

        /*
         * Construyo el enlace de confirmación usando la URL guardada en base de datos.
         * Así no dejo la URL escrita directamente en el código.
         */
        String confirmBaseUrl = this.appUrlService.getUrl("BACKEND_CONFIRM_TOKEN_BASE_URL");

        String link =
            this.appUrlService.join(confirmBaseUrl, encode(user.getEmail())) +
            "?token=" +
            encode(token.getId());

        /*
         * Mensaje que se enviará por correo al usuario.
         */
        String mensaje =
            "🎶 GRAMOLA 🎶\n\n" +
            "¡Bienvenido/a!\n\n" +
            "Para confirmar tu cuenta, pulsa aquí:\n" +
            link + "\n\n" +
            "Si no fuiste tú, ignora este correo.\n\n" +
            "— Equipo Gramola 🎧";

        /*
         * Intento enviar el correo de confirmación.
         */
        try {
            this.emailService.send(email, "Confirma tu cuenta - Gramola", mensaje);
        } catch (Exception e) {
            System.err.println("ERROR enviando email: " + e.getMessage());
            e.printStackTrace();
        }

        /*
         * Devuelvo el id del token por si el frontend lo necesita.
         */
        return token.getId();
    }

    /*
     * Inicio sesión con email y contraseña.
     *
     * El frontend manda la contraseña real, yo la hasheo y comparo
     * con la contraseña guardada en base de datos.
     */
    public User login(String email, String pwd) {
        if (email == null || pwd == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing email or pwd");
        }

        /*
         * Calculo el hash de la contraseña recibida.
         */
        String hash1 = PasswordUtil.hash(pwd);

        /*
         * Busco un usuario que tenga ese email y esa contraseña hasheada.
         */
        User user = this.userDao.findByEmailAndPwd(email, hash1);

        /*
         * Mantengo esta compatibilidad por si algún usuario quedó guardado
         * con doble hash durante pruebas anteriores.
         */
        if (user == null) {
            String hash2 = PasswordUtil.hash(hash1);
            user = this.userDao.findByEmailAndPwd(email, hash2);

            if (user != null) {
                /*
                 * Si encuentro un usuario con doble hash, corrijo su contraseña
                 * para dejarla con un solo hash.
                 */
                user.setPwd(hash1);
                this.userDao.save(user);
            }
        }

        /*
         * Si no encuentro usuario, las credenciales son incorrectas.
         */
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bad credentials");
        }

        Token token = user.getCreationToken();

        /*
         * Si el token de creación no está usado, significa que el usuario
         * todavía no confirmó su cuenta por correo.
         */
        if (token != null && !token.isUsed()) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "User not confirmed");
        }

        /*
         * Si no ha pagado la suscripción, no le dejo iniciar sesión.
         */
        if (!user.isSubscriptionPaid()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Subscription not paid");
        }

        return user;
    }

    /*
     * Borro un usuario por email.
     */
    public void delete(String email) {
        Optional<User> opt = this.userDao.findById(email);

        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        this.userDao.delete(opt.get());
    }

    /*
     * Devuelvo el id del token de creación asociado a un usuario.
     */
    public String getTokenIdForUser(String email) {
        Optional<User> opt = this.userDao.findById(email);

        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        User user = opt.get();

        if (user.getCreationToken() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Token not found");
        }

        return user.getCreationToken().getId();
    }

    /*
     * Confirmo la cuenta de un usuario.
     *
     * Este método se ejecuta cuando el usuario pulsa el enlace del correo.
     */
    public void confirmToken(String email, String token) {
        Optional<User> opt = this.userDao.findById(email);

        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        User user = opt.get();
        Token userToken = user.getCreationToken();

        /*
         * Compruebo que el usuario tenga token de creación.
         */
        if (userToken == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Token not found");
        }

        /*
         * Compruebo que el token recibido coincida con el token del usuario.
         */
        if (!userToken.getId().equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        /*
         * Compruebo que el token no haya caducado.
         * En este caso uso una caducidad de 30 minutos.
         */
        if (userToken.getCreationTime() < System.currentTimeMillis() - 1800000) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired");
        }

        /*
         * Compruebo que el token no se haya usado ya.
         */
        if (userToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token already used");
        }

        /*
         * Marco el token como usado y guardo el usuario.
         */
        userToken.use();
        this.userDao.save(user);
    }

    /*
     * Métodos antiguos para acceder al mapa en memoria.
     * La información real ya se maneja con MySQL, no con este Map.
     */
    public Map<String, User> getUsers() {
        return users;
    }

    public void setUsers(Map<String, User> users) {
        this.users = users;
    }

    /*
     * Solicito recuperación de contraseña.
     *
     * Si el email existe, creo un token de recuperación y envío un correo
     * con el enlace para cambiar la contraseña.
     */
    public void requestPasswordReset(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing email");
        }

        email = email.trim();

        Optional<User> opt = this.userDao.findById(email);

        /*
         * No lanzo error si el email no existe para no revelar usuarios registrados.
         */
        if (opt.isEmpty()) {
            return;
        }

        User user = opt.get();

        /*
         * Creo un nuevo token de recuperación y lo asocio al usuario.
         */
        Token resetToken = new Token();
        user.setResetToken(resetToken);
        this.userDao.save(user);

        /*
         * Construyo el enlace de recuperación con la URL del frontend guardada en BD.
         */
        String resetPasswordUrl = this.appUrlService.getUrl("FRONTEND_RESET_PASSWORD_URL");

        String link =
            resetPasswordUrl +
            "?email=" + encode(user.getEmail()) +
            "&token=" + encode(resetToken.getId());

        /*
         * Mensaje de recuperación de contraseña.
         */
        String mensaje =
            "🎶 GRAMOLA 🎶\n\n" +
            "Has solicitado recuperar tu contraseña.\n\n" +
            "Pulsa aquí para cambiarla:\n" +
            link + "\n\n" +
            "Este enlace caduca en 30 minutos.\n" +
            "Si no fuiste tú, ignora este correo.\n\n" +
            "— Equipo Gramola 🎧";

        /*
         * Envío el correo de recuperación.
         */
        try {
            this.emailService.send(user.getEmail(), "Recuperar contraseña - Gramola", mensaje);
        } catch (Exception e) {
            System.err.println("ERROR enviando email reset: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * Cambio la contraseña usando un token de recuperación.
     */
    public void resetPassword(String email, String token, String pwd1, String pwd2) {
        if (email == null || token == null || pwd1 == null || pwd2 == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fields");
        }

        email = email.trim();

        /*
         * Compruebo que las dos contraseñas coincidan.
         */
        if (!pwd1.equals(pwd2)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Passwords do not match");
        }

        /*
         * Compruebo longitud mínima.
         */
        if (pwd1.length() < 8) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Password must be at least 8 characters long");
        }

        Optional<User> opt = this.userDao.findById(email);

        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        User user = opt.get();
        Token resetToken = user.getResetToken();

        /*
         * Compruebo que exista token de recuperación.
         */
        if (resetToken == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reset token not found");
        }

        /*
         * Compruebo que el token recibido sea el correcto.
         */
        if (!resetToken.getId().equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        /*
         * Compruebo que no haya caducado.
         */
        if (resetToken.isExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired");
        }

        /*
         * Compruebo que no se haya usado antes.
         */
        if (resetToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token already used");
        }

        /*
         * Cambio la contraseña guardando su hash y marco el token como usado.
         */
        user.setPwd(PasswordUtil.hash(pwd1));
        resetToken.use();

        this.userDao.save(user);
    }

    /*
     * Codifico valores para poder meterlos en una URL.
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}