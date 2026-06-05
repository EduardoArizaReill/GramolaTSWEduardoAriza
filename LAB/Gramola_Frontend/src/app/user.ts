/* CHECK
 * Servicio encargado de las operaciones de usuario en el frontend.
 *
 * Desde aquí hago las llamadas al backend para registrar un bar,
 * iniciar sesión y borrar un usuario.
 *
 * No escribo la URL base directamente en cada método, sino que la pido
 * a AppUrlService usando la clave USERS_BASE_URL.
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, switchMap } from 'rxjs';

import { AppUrlService } from './app-url.service';

/*
 * Interfaz para el cuerpo del login.
 * El frontend manda email y contraseña al backend.
 */
export interface LoginRequest {
  email: string;
  pwd: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(
    private http: HttpClient,
    private appUrlService: AppUrlService
  ) {}

  /*
   * Registro un bar nuevo.
   *
   * Envío al backend los datos como JSON normal.
   * El backend los recibe en UserController mediante un Map.
   */
  register(
    email: string,
    pwd1: string,
    pwd2: string,
    bar = '',
    clientId = '',
    clientSecret = ''
  ): Observable<any> {

    /*
     * Creo el cuerpo que voy a enviar al backend.
     * Aquí van los datos del formulario de registro.
     */
    const body = {
      bar,
      email,
      pwd1,
      pwd2,
      clientId,
      clientSecret
    };

    /*
     * Primero pido la URL base de usuarios.
     * Después hago el POST al endpoint /register.
     */
    return this.appUrlService.getUrl('USERS_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.post<any>(
          `${baseUrl}/register`,
          body,
          { withCredentials: true }
        )
      )
    );
  }

  /*
   * Inicio sesión con email y contraseña.
   */
  login(email: string, pwd: string): Observable<any> {
    const body: LoginRequest = {
      email,
      pwd
    };

    /*
     * Pido la URL base de usuarios y llamo al endpoint /login.
     */
    return this.appUrlService.getUrl('USERS_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.post<any>(
          `${baseUrl}/login`,
          body,
          { withCredentials: true }
        )
      )
    );
  }

  /*
   * Borro un usuario por email.
   *
   * Uso encodeURIComponent para que el email viaje correctamente
   * dentro de la URL aunque tenga caracteres especiales.
   */
  deleteUser(email: string): Observable<any> {
    return this.appUrlService.getUrl('USERS_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.delete<any>(
          `${baseUrl}/delete?email=${encodeURIComponent(email)}`,
          { withCredentials: true }
        )
      )
    );
  }
}