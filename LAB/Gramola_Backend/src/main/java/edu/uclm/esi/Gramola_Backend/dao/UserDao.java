/*
 * Repositorio encargado de acceder a la tabla user.
 *
 * Esta clase me permite buscar usuarios por email, contraseña,
 * credenciales de Spotify o token de confirmación.
 * Es una de las clases más importantes porque casi todo el proyecto
 * depende del usuario/bar registrado.
 */
package edu.uclm.esi.Gramola_Backend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.esi.Gramola_Backend.model.User;

@Repository
public interface UserDao extends JpaRepository<User, String> {

    /*
     * Busco un usuario por email y contraseña ya hasheada.
     * Lo uso en el login para comprobar si las credenciales son correctas.
     */
    User findByEmailAndPwd(String email, String pwd);

    /*
     * Busco un usuario por su clientId de Spotify.
     * Puede servir si necesito localizar el bar asociado a unas credenciales de Spotify.
     */
    User findByClientId(String clientId);

    /*
     * Busco un usuario usando el id de su token de creación.
     * Esto lo uso cuando el usuario confirma la cuenta o paga la suscripción
     * después de entrar desde el enlace del correo.
     */
    User findByCreationToken_Id(String id);

    /*
     * Método alternativo para buscar por token de creación.
     * Hace prácticamente lo mismo que findByCreationToken_Id.
     * Si no se usa en ningún sitio, se podría eliminar para dejar el DAO más limpio.
     */
    User findByCreationTokenId(String creationTokenId);
}