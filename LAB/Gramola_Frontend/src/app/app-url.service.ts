
/* CHECK
 * Servicio encargado de cargar las URLs de la aplicación desde el backend. 
 * sin esto se apaga
 *
 * Esta clase es importante porque evita que el frontend tenga muchas URLs
 * escritas directamente en el código. Solo dejo una URL inicial para pedir
 * al backend todas las demás URLs guardadas en MySQL.
 *
 * A partir de este servicio, otras clases como UserService, PaymentService
 * o SpotiService pueden pedir la URL que necesitan usando una clave.
 */
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, shareReplay } from 'rxjs';

export type AppUrls = Record<string, string>;

@Injectable({
  providedIn: 'root'
})
export class AppUrlService {

  /*
   * Necesito una URL inicial para pedir al backend todas las URLs de MySQL.
   * A partir de aquí, el resto de URLs ya salen de la base de datos.
   */
  private readonly appUrlsEndpoint = 'http://127.0.0.1:8080/appUrls';

  private urls$?: Observable<AppUrls>;

  constructor(private http: HttpClient) {}

  /*
   * Cargo todas las URLs activas desde el backend.
   * Uso shareReplay para no repetir la misma petición cada vez que un servicio necesita una URL.
   */
  getAllUrls(): Observable<AppUrls> {
    if (!this.urls$) {
      this.urls$ = this.http.get<AppUrls>(
        this.appUrlsEndpoint,
        { withCredentials: true }
      ).pipe(
        shareReplay(1)
      );
    }

    return this.urls$;
  }

  /*
   * Devuelvo una URL concreta por su clave.
   * Si falta en base de datos, lanzo error para verlo rápido.
   */
  getUrl(key: string): Observable<string> {
    return this.getAllUrls().pipe(
      map((urls) => {
        const url = urls[key];

        if (!url) {
          throw new Error(`No existe la URL en base de datos: ${key}`);
        }

        return url;
      })
    );
  }
}