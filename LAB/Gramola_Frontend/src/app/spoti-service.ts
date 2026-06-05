/* CHECK
 * Servicio encargado de las operaciones de Spotify en el frontend.
 *
 * Esta clase usa el token de Spotify guardado en sessionStorage
 * para pedir playlists, dispositivos, canción actual, buscar canciones
 * y enviar canciones a la cola real de Spotify.
 *
 * Algunas peticiones van al backend y otras van directamente a Spotify,
 * pero las URLs se obtienen desde AppUrlService para no dejarlas hardcodeadas.
 */

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, switchMap } from 'rxjs';

import { AppUrlService } from './app-url.service';

@Injectable({
  providedIn: 'root'
})
export class SpotiService {

  constructor(
    private http: HttpClient,
    private appUrlService: AppUrlService
  ) {}

  /*
   * Cojo el token de Spotify que guardé después del callback.
   */
  private getSpotifyToken(): string {
    return sessionStorage.getItem('SpotifyToken') || '';
  }

  /*
   * Creo los headers de Spotify en un solo sitio.
   * Así no repito el Authorization en cada método.
   */
  private getSpotifyHeaders(): HttpHeaders {
    return new HttpHeaders({
      Authorization: `Bearer ${this.getSpotifyToken()}`
    });
  }

  /*
   * Intercambio el code de Spotify por un token.
   *
   * Cuando Spotify devuelve el code al callback, llamo al backend
   * para que intercambie ese code por un access token.
   */
  getAuthorizationToken(code: string, email: string): Observable<any> {
    return this.appUrlService.getUrl('API_SPOTI_BASE_URL').pipe(
      switchMap((baseUrl) => {
        const url =
          `${baseUrl}/getAuthorizationToken` +
          `?code=${encodeURIComponent(code)}` +
          `&email=${encodeURIComponent(email)}`;

        return this.http.get<any>(url);
      })
    );
  }

  /*
   * Obtengo los dispositivos disponibles de Spotify.
   */
  getDevices(): Observable<any> {
    return this.appUrlService.getUrl('API_SPOTI_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.get<any>(
          `${baseUrl}/me/player/devices`,
          { headers: this.getSpotifyHeaders() }
        )
      )
    );
  }

  /*
   * Obtengo las playlists del usuario conectado.
   */
  getPlaylists(): Observable<any> {
    return this.appUrlService.getUrl('API_SPOTI_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.get<any>(
          `${baseUrl}/me/playlists`,
          { headers: this.getSpotifyHeaders() }
        )
      )
    );
  }

  /*
   * Obtengo la canción que está sonando actualmente.
   */
  getCurrentPlayList(): Observable<any> {
    return this.appUrlService.getUrl('API_SPOTI_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.get<any>(
          `${baseUrl}/me/player/currently-playing`,
          { headers: this.getSpotifyHeaders() }
        )
      )
    );
  }

  /*
   * Busco canciones por texto.
   */
  searchTracks(query: string): Observable<any> {
    return this.appUrlService.getUrl('API_SPOTI_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.get<any>(
          `${baseUrl}/search?query=${encodeURIComponent(query)}`,
          { headers: this.getSpotifyHeaders() }
        )
      )
    );
  }

  /*
   * Envío una canción a la cola real de Spotify.
   * La URL directa de Spotify también viene desde MySQL.
   */
  addSongToSpotifyQueue(trackUri: string, deviceId?: string): Observable<any> {
    return this.appUrlService.getUrl('SPOTIFY_QUEUE_URL').pipe(
      switchMap((queueUrl) => {
        let url = `${queueUrl}?uri=${encodeURIComponent(trackUri)}`;

        /*
         * Si el usuario eligió un dispositivo concreto, lo añado a la URL.
         */
        if (deviceId?.trim()) {
          url += `&device_id=${encodeURIComponent(deviceId)}`;
        }

        /*
         * Spotify espera un POST sin body para añadir a la cola.
         */
        return this.http.post(url, null, {
          headers: this.getSpotifyHeaders(),
          responseType: 'text'
        });
      })
    );
  }
}