/* CHECK
 * Componente encargado de registrar un nuevo bar.
 *
 * Esta pantalla recoge los datos necesarios para crear una cuenta:
 * nombre del bar, email, credenciales de Spotify y contraseña.
 *
 * Antes de llamar al backend hago unas validaciones básicas en el frontend.
 * Si todo está correcto, llamo a UserService.register(), que enviará los datos
 * al backend para crear el usuario y mandar el correo de confirmación.
 */
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../user';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register-component.html',
  styleUrl: './register-component.css'
})
export class RegisterComponent {

  bar: string = '';
  email: string = '';
  clientId: string = '';
  clientSecret: string = '';
  pwd1: string = '';
  pwd2: string = '';

  registroOK: string | null = null;
  registroKO: string | null = null;

  constructor(private service: UserService) {}

  registrar() {
    // ---- Validación mínima (front) ----
    if (!this.bar.trim()) {
      this.registroKO = 'El nombre del bar es obligatorio.';
      this.registroOK = null;
      return;
    }

    if (!this.email.trim()) {
      this.registroKO = 'El email es obligatorio.';
      this.registroOK = null;
      return;
    }

    if (!this.clientId.trim()) {
      this.registroKO = 'El Client ID de Spotify es obligatorio.';
      this.registroOK = null;
      return;
    }

    if (!this.clientSecret.trim()) {
      this.registroKO = 'El Client Secret de Spotify es obligatorio.';
      this.registroOK = null;
      return;
    }

    // OJO: mínimo 8 (en UserController)
    if (!this.pwd1 || this.pwd1.length < 8) {
      this.registroKO = 'La contraseña debe tener al menos 8 caracteres.';
      this.registroOK = null;
      return;
    }

    if (this.pwd1 !== this.pwd2) {
      this.registroKO = 'Las contraseñas no coinciden.';
      this.registroOK = null;
      return;
    }

    // ---- Llamada al backend, ahora pasando clientId y clientSecret ----
    this.service.register(
      this.email.trim(),
      this.pwd1,
      this.pwd2,
      this.bar.trim(),
      this.clientId.trim(),
      this.clientSecret.trim()
    ).subscribe({
      next: () => {
        this.registroOK = 'Registro realizado con éxito. Revisa tu correo para confirmar la cuenta.';
        this.registroKO = null;

        // limpiar campos
        this.bar = '';
        this.email = '';
        this.clientId = '';
        this.clientSecret = '';
        this.pwd1 = '';
        this.pwd2 = '';
      },
      error: (error) => {
        const backendMsg = error?.error?.message || error?.message || 'Error desconocido';
        this.registroKO = backendMsg;
        this.registroOK = null;
      }
    });
  }
}
