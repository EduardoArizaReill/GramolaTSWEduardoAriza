/* CHECK
 * Servicio encargado de gestionar los pagos desde el frontend.
 *
 * Esta clase habla con el backend de pagos para pedir planes,
 * pedir el precio por canción, preparar un pago con Stripe
 * y confirmar la transacción cuando Stripe responde correctamente.
 *
 * La URL base de pagos no está escrita directamente aquí,
 * sino que la obtengo desde AppUrlService usando PAYMENTS_BASE_URL.
 */

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, switchMap } from 'rxjs';

import { AppUrlService } from './app-url.service';

/*
 * Interfaz que representa un plan de pago recibido desde el backend.
 */
export interface PaymentPlan {
  id: string;
  name: string;
  description?: string;
  amount: number;
  currency: string;
  planType?: string;
  intervalType?: string;
  recommended?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {

  constructor(
    private http: HttpClient,
    private appUrlService: AppUrlService
  ) {}

  /*
   * Cojo el email del bar guardado en el navegador.
   * Lo uso para pedir precios concretos de ese bar.
   */
  private getBarEmail(): string {
    return (
      localStorage.getItem('email') ||
      sessionStorage.getItem('email') ||
      ''
    ).trim();
  }

  /*
   * Pido al backend los planes de suscripción.
   * La URL base de pagos viene de MySQL.
   */
  getPlans(): Observable<PaymentPlan[]> {
    return this.appUrlService.getUrl('PAYMENTS_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.get<PaymentPlan[]>(
          `${baseUrl}/plans`,
          { withCredentials: true }
        )
      )
    );
  }

  /*
   * Pido el precio de canción.
   * Si hay email del bar, el backend puede devolver su precio personalizado.
   */
  getSongPlan(): Observable<PaymentPlan> {
    const email = this.getBarEmail();

    return this.appUrlService.getUrl('PAYMENTS_BASE_URL').pipe(
      switchMap((baseUrl) => {
        const url = email
          ? `${baseUrl}/songPlan?email=${encodeURIComponent(email)}`
          : `${baseUrl}/songPlan`;

        return this.http.get<PaymentPlan>(
          url,
          { withCredentials: true }
        );
      })
    );
  }

  /*
   * Preparo un pago en Stripe a través del backend.
   *
   * No llamo directamente a Stripe desde aquí para crear la transacción.
   * Se lo pido al backend, porque el backend tiene la lógica segura
   * y la clave secreta de Stripe.
   */
  prepay(
    plan: string,
    token?: string,
    email?: string,
    songPriceCents?: number
  ): Observable<any> {
    const params = new URLSearchParams();

    /*
     * Indico qué plan se quiere pagar: mensual, anual o canción.
     */
    params.set('plan', plan);

    /*
     * Si hay token, lo mando.
     * Se usa sobre todo cuando el usuario viene desde el enlace de confirmación.
     */
    if (token) {
      params.set('token', token);
    }

    /*
     * Uso el email recibido o, si no viene, el email guardado en el navegador.
     */
    const finalEmail = email || this.getBarEmail();

    if (finalEmail) {
      params.set('email', finalEmail);
    }

    /*
     * Si el bar ha elegido un precio por canción, también lo envío.
     */
    if (songPriceCents != null && songPriceCents > 0) {
      params.set('songPriceCents', String(songPriceCents));
    }

    /*
     * Pido al backend que prepare el pago.
     */
    return this.appUrlService.getUrl('PAYMENTS_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.get<any>(
          `${baseUrl}/prepay?${params.toString()}`,
          { withCredentials: true }
        )
      )
    );
  }

  /*
   * Confirmo el pago en el backend.
   *
   * Después de que Stripe confirme el pago en el frontend,
   * mando los datos finales al backend para que valide la transacción.
   */
  confirm(finalData: any, transactionId: string, token?: string): Observable<any> {
    const payload: any = {
      ...finalData,
      transactionId
    };

    if (token) {
      payload.token = token;
    }

    return this.appUrlService.getUrl('PAYMENTS_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.post<any>(
          `${baseUrl}/confirm`,
          payload,
          { withCredentials: true }
        )
      )
    );
  }
}