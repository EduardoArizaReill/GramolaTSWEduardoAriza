/* CHECK
 * Componente encargado de iniciar sesión.
 *
 * Esta pantalla muestra el formulario de login, valida email y contraseña,
 * llama al servicio de usuario para comprobar las credenciales en el backend
 * y, si todo va bien, guarda los datos básicos del bar en el navegador.
 *
 * No llamo directamente al backend desde aquí. Uso UserService para mantener
 * separada la lógica de la pantalla y la lógica de comunicación HTTP.
 */
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { UserService } from '../user';

@Component({
  selector: 'app-login-componente',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login-componente.html',
  styleUrl: './login-componente.css'
})
export class LoginComponente {

  errMsg: string | null = null;
  loginForm: FormGroup;

  constructor(
    private userService: UserService,
    private fb: FormBuilder,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email, Validators.minLength(5)]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  /*
   * Llevo al usuario a la pantalla de recuperar contraseña.
   * Lo hago con router.navigate porque no estoy usando routerLink en el HTML.
   */
  goForgotPassword(): void {
    this.router.navigate(['/forgot-password']);
  }

  /*
   * Si el formulario es válido, intento iniciar sesión.
   * Después guardo los datos básicos del bar para usarlos en el resto de pantallas.
   */
  onSubmit(): void {
    if (this.loginForm.invalid) {
      return;
    }

    const email = this.loginForm.get('email')!.value;
    const password = this.loginForm.get('password')!.value;

    this.userService.login(email, password).subscribe({
      next: (response: any) => {
        this.errMsg = null;

        this.saveSessionData(response);

        this.router.navigate(['/']);
      },
      error: (err: any) => {
        this.errMsg = err?.error?.message || err?.message || 'Error en login';
      }
    });
  }

  /*
   * Guardo los datos en localStorage y sessionStorage.
   * Así los puedo recuperar aunque refresque la página.
   */
  private saveSessionData(response: any): void {
    this.saveValue('email', response.email);
    this.saveValue('barName', response.barName);
    this.saveValue('clientId', response.clientId);
    this.saveValue('subscriptionPaid', String(response.subscriptionPaid));
  }

  private saveValue(key: string, value: string): void {
    localStorage.setItem(key, value);
    sessionStorage.setItem(key, value);
  }
}