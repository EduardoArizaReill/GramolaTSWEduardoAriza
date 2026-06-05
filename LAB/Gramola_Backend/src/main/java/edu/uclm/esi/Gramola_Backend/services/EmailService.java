/* CHECK
 * Servicio encargado de enviar correos electrónicos
 * Sin esto nos olvidamos de hacer cualquiermensaje al correo o 
 * cualquiercosa con el correo.
 *
 * Esta clase se usa principalmente para mandar correos de confirmación
 * de cuenta y correos de recuperación de contraseña.
 *
 * Centralizo aquí el envío de emails para que UserService no tenga que saber
 * cómo se construye técnicamente un correo.
 */
package edu.uclm.esi.Gramola_Backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    /*
     * Objeto de Spring encargado de enviar realmente el correo.
     * Su configuración está en application.properties.
     */
    @Autowired
    private JavaMailSender emailSender;

    /*
     * Envío un correo simple indicando destinatario, asunto y texto.
     */
    public void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();

        /*
         * Indico el remitente, destinatario, asunto y contenido del correo.
         */
        message.setFrom("gramola-app@noreply.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        /*
         * Envío el correo usando JavaMailSender.
         */
        emailSender.send(message);

        /*
         * Muestro por consola que el correo se ha enviado correctamente.
         * Esto me ayuda a depurar durante las pruebas.
         */
        System.out.println("Email enviado correctamente a " + to);
    }
}