/* CHECK
 * Componente encargado de cambiar la contraseña del usuario.
 *
 * Esta pantalla se abre desde el enlace de recuperación que llega por correo.
 * En ese enlace vienen el email y el token en la URL.
 *
 * El usuario introduce la nueva contraseña dos veces y este componente
 * manda los datos al backend para que valide el token y actualice la contraseña.
 */
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { switchMap } from 'rxjs';

import { AppUrlService } from '../app-url.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent {
  email = '';
  token = '';
  pwd1 = '';
  pwd2 = '';
  msg = '';

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private appUrlService: AppUrlService
  ) {
    this.route.queryParams.subscribe((p) => {
      this.email = p['email'] || '';
      this.token = p['token'] || '';
    });
  }

  /*
   * Cambio la contraseña usando la URL guardada en MySQL.
   * Así no dejo el endpoint escrito directamente en el componente.
   */
  submit(): void {
    const body = {
      email: this.email,
      token: this.token,
      pwd1: this.pwd1,
      pwd2: this.pwd2
    };

    this.appUrlService.getUrl('USER_RESET_PASSWORD_URL').pipe(
      switchMap((url) =>
        this.http.post<any>(
          url,
          body,
          { withCredentials: true }
        )
      )
    ).subscribe({
      next: (r) => {
        this.msg = r?.message ?? 'Contraseña actualizada.';
      },
      error: (e) => {
        this.msg = e?.error?.message ?? 'Error';
      }
    });
  }
}