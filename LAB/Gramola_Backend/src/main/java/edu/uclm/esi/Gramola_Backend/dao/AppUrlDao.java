/* CHECK
 * Repositorio encargado de acceder a la tabla app_url.
 *
 * Esta clase me permite consultar las URLs que tengo guardadas en base de datos.
 * La uso para evitar tener URLs escritas directamente en el código.
 */
package edu.uclm.esi.Gramola_Backend.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uclm.esi.Gramola_Backend.model.AppUrl;

public interface AppUrlDao extends JpaRepository<AppUrl, String> {

    /*
     * Busco una URL concreta por su id, pero solo si está activa.
     * Así evito usar una URL que haya sido deshabilitada en base de datos.
     */
    Optional<AppUrl> findByIdAndActiveTrue(String id);

    /*
     * Recupero todas las URLs activas.
     * Esto lo uso para enviarlas al frontend y que Angular pueda trabajar
     * sin tener las URLs hardcodeadas.
     */
    List<AppUrl> findByActiveTrue();
}