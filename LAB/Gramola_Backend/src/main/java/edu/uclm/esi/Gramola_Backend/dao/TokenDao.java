package edu.uclm.esi.Gramola_Backend.dao;
/*CHECK
 * Repositorio encargado de acceder a la tabla token.
 *
 * Esta clase me permite guardar y consultar tokens de confirmación de cuenta
 * y tokens de recuperación de contraseña.
 */
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uclm.esi.Gramola_Backend.model.User;

@Repository
public interface  TokenDao extends JpaRepository <User, String> {

}
