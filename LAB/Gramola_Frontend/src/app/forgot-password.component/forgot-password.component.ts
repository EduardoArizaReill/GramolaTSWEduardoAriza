/* CHECK
 * Componente encargado de solicitar la recuperación de contraseña.
 *
 * Esta pantalla permite que el usuario escriba su email para recibir
 * un correo con un enlace de recuperación.
 *
 * No tiene la lógica de cambiar la contraseña. Solo pide al backend
 * que genere un token de recuperación y envíe el correo correspondiente.
 */
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { switchMap } from 'rxjs';

import { AppUrlService } from '../app-url.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  email = '';
  msg = '';

  constructor(
    private http: HttpClient,
    private appUrlService: AppUrlService
  ) {}

  /*
   * Pido al backend que mande el correo de recuperación.
   * La URL sale de MySQL, no del componente.
   */
  submit(): void {
    this.appUrlService.getUrl('USER_FORGOT_PASSWORD_URL').pipe(
      switchMap((url) =>
        this.http.post<any>(
          url,
          { email: this.email },
          { withCredentials: true }
        )
      )
    ).subscribe({
      next: (r) => {
        this.msg = r?.message ?? 'Revisa tu correo.';
      },
      error: (e) => {
        this.msg = e?.error?.message ?? 'Error';
      }
    });
  }
}