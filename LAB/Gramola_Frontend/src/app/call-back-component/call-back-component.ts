/* CHECK
 * Componente encargado de gestionar el callback de Spotify.
 *
 * Cuando el usuario autoriza la aplicación en Spotify, Spotify lo redirige
 * a esta pantalla con un code en la URL. Este componente recoge ese code,
 * recupera el email del bar y pide al backend que intercambie el code
 * por un token de acceso de Spotify.
 *
 * Si todo va bien, guarda el token en sessionStorage y redirige al usuario
 * a la pantalla de música.
 */

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SpotiService } from '../spoti-service';
import { CommonModule } from '@angular/common';

/*
 * Defino los posibles estados visuales de la pantalla.
 * loading: se está conectando.
 * success: la conexión fue correcta.
 * error: hubo algún problema.
 */
type Status = 'loading' | 'error' | 'success';

@Component({
  selector: 'app-call-back-component',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './call-back-component.html',
  styleUrl: './call-back-component.css'
})
export class CallBackComponent implements OnInit {

  /*
   * Estado inicial de la pantalla.
   * Al entrar aquí, primero muestro que se está conectando con Spotify.
   */
  status: Status = 'loading';

  /*
   * Mensaje de error que se mostrará en el HTML si algo falla.
   */
  errorMsg: string | null = null;

  constructor(
    /*
     * ActivatedRoute me permite leer los parámetros que llegan en la URL,
     * por ejemplo code, state o error.
     */
    private route: ActivatedRoute,

    /*
     * Router me permite navegar a otras pantallas desde código.
     */
    private router: Router,

    /*
     * Servicio que se comunica con el backend para gestionar Spotify.
     */
    public spoti: SpotiService
  ) {}

  /*
   * Este método se ejecuta automáticamente cuando se carga el componente.
   */
  ngOnInit(): void {
    /*
     * Leo los parámetros de la URL.
     * Spotify puede devolver code, state o error.
     */
    const qp = this.route.snapshot.queryParamMap;

    const code = qp.get('code');
    const state = qp.get('state');
    const error = qp.get('error');

    /*
     * Si Spotify devuelve error, paro el proceso y muestro mensaje.
     */
    if (error) {
      this.status = 'error';
      this.errorMsg = 'No se pudo conectar con Spotify. Se canceló el permiso o Spotify devolvió un error.';
      return;
    }

    /*
     * Si no llega code, no puedo pedir el token de Spotify.
     */
    if (!code) {
      this.status = 'error';
      this.errorMsg = 'No ha llegado el código de Spotify. Vuelve a conectar.';
      return;
    }

    /*
     * Intento extraer el email desde el parámetro state.
     * El backend construye state con formato email|uuid.
     */
    const emailFromState = this.extractEmailFromState(state);

    /*
     * Si no consigo email desde state, intento cogerlo del navegador.
     */
    const email =
      emailFromState ||
      localStorage.getItem('email') ||
      sessionStorage.getItem('email') ||
      '';

    /*
     * Si no tengo email, no sé qué bar está conectando Spotify.
     */
    if (!email.trim()) {
      this.status = 'error';
      this.errorMsg = 'No se pudo saber qué bar está conectando Spotify. Vuelve a iniciar sesión.';
      return;
    }

    /*
     * Guardo el email en localStorage y sessionStorage para usarlo después.
     */
    localStorage.setItem('email', email.trim());
    sessionStorage.setItem('email', email.trim());

    /*
     * Limpio la URL del navegador para que no se queden visibles el code y el state.
     */
    history.replaceState({}, '', '/callback');

    /*
     * Pido al backend que intercambie el code por un token de Spotify.
     */
    this.spoti.getAuthorizationToken(code, email.trim()).subscribe({
      next: (data: any) => {
        /*
         * Si Spotify no devuelve access_token, considero que ha fallado.
         */
        if (!data?.access_token) {
          this.status = 'error';
          this.errorMsg = 'Spotify no devolvió un token válido.';
          return;
        }

        /*
         * Guardo el token de Spotify para que otros servicios puedan usarlo.
         */
        sessionStorage.setItem('SpotifyToken', data.access_token);

        /*
         * Cambio el estado visual a success.
         */
        this.status = 'success';

        /*
         * Después de un pequeño tiempo, llevo al usuario a la pantalla de música.
         */
        setTimeout(() => {
          this.router.navigateByUrl('/music');
        }, 650);
      },

      error: (err: any) => {
        console.error('Error fetching access token:', err);

        this.status = 'error';

        /*
         * Si el backend responde 404, significa que no encontró el bar en BD.
         */
        if (err.status === 404) {
          this.errorMsg = 'No existe en la base de datos el bar que intenta conectar con Spotify.';
          return;
        }

        /*
         * Si responde 400, suele ser problema de credenciales o redirect URI.
         */
        if (err.status === 400) {
          this.errorMsg = 'Faltan credenciales de Spotify para este bar o el redirect URI no coincide.';
          return;
        }

        /*
         * Error genérico para cualquier otro problema.
         */
        this.errorMsg = 'Error obteniendo el token de Spotify. Revisa el backend, el client_id/client_secret o el redirect URI.';
      }
    });
  }

  /*
   * Extraigo el email desde el parámetro state.
   * El formato esperado es email|uuid.
   */
  private extractEmailFromState(state: string | null): string {
    if (!state) {
      return '';
    }

    const parts = state.split('|');

    if (parts.length === 0) {
      return '';
    }

    return parts[0] || '';
  }

  /*
   * Llevo al usuario a la pantalla inicial.
   */
  goHome(): void {
    this.router.navigateByUrl('/');
  }

  /*
   * Permito volver a intentar la conexión.
   * Ahora mismo lo mando al inicio.
   */
  retry(): void {
    this.router.navigateByUrl('/');
  }
}