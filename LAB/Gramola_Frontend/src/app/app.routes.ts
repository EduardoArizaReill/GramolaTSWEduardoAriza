/* CHECK
 * Archivo donde defino las rutas principales del frontend.
 *
 * Cada ruta indica qué componente debe mostrarse cuando el usuario entra
 * en una dirección concreta de Angular.
 *
 * Estas rutas se cargan dentro del router-outlet del componente raíz.
 */

import { Routes } from '@angular/router';

import { WelcomeComponent } from './welcome-component/welcome-component';
import { RegisterComponent } from './register-component/register-component';
import { PaymentComponent } from './payments-component/payments-componente';
import { LoginComponente } from './login-component/login-componente';
import { DevicesAndPlaylistsComponent } from './devices-and-playlists-component/devices-and-playlists-component';
import { CallBackComponent } from './call-back-component/call-back-component';
import { MusicComponent } from './music-component/music-component';
import { ForgotPasswordComponent } from './forgot-password.component/forgot-password.component';
import { ResetPasswordComponent } from './reset-password.component/reset-password.component';

/*
 * Aquí defino las rutas principales de la aplicación.
 * Cada path carga un componente distinto dentro del router-outlet.
 */
export const routes: Routes = [
  /*
   * Pantalla inicial.
   */
  { path: '', component: WelcomeComponent, pathMatch: 'full' },

  /*
   * Rutas de usuario.
   */
  { path: 'login', component: LoginComponente },
  { path: 'register', component: RegisterComponent },

  /*
   * Ruta de pago.
   */
  { path: 'payment', component: PaymentComponent },

  /*
   * Rutas relacionadas con Spotify y música.
   */
  { path: 'music', component: MusicComponent },
  { path: 'devicesAndPlaylists', component: DevicesAndPlaylistsComponent },
  { path: 'callback', component: CallBackComponent },

  /*
   * Rutas para recuperar contraseña.
   */
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },

  /*
   * Si alguien entra en una ruta que no existe,
   * lo devuelvo a la pantalla inicial.
   */
  { path: '**', redirectTo: '' }
];