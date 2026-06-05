/* CHECK
 * Componente de bienvenida de la aplicación.
 *
 * Esta pantalla funciona como inicio del frontend.
 * Muestra el nombre del bar si está guardado en el navegador
 * y permite iniciar la conexión del bar con Spotify.
 *
 * Para conectar con Spotify, obtiene desde MySQL la URL del backend
 * usando AppUrlService y después redirige al usuario a esa URL.
 */
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AppUrlService } from '../app-url.service';

@Component({
  selector: 'app-welcome-component',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './welcome-component.html',
  styleUrl: './welcome-component.css'
})
export class WelcomeComponent implements OnInit {

  barName = 'Gramola';

  constructor(private appUrlService: AppUrlService) {}

  ngOnInit(): void {
    this.barName =
      localStorage.getItem('barName') ||
      sessionStorage.getItem('barName') ||
      'Gramola';
  }

  /*
   * Conecto el bar con Spotify.
   * La URL base del endpoint sale de MySQL.
   */
  conectarSpotify(): void {
    const email =
      localStorage.getItem('email') ||
      sessionStorage.getItem('email') ||
      '';

    if (!email.trim()) {
      alert('No hay usuario logueado. Inicia sesión antes de conectar con Spotify.');
      window.location.href = '/login';
      return;
    }

    this.appUrlService.getUrl('SPOTI_CONNECT_URL').subscribe({
      next: (connectUrl) => {
        window.location.href =
          `${connectUrl}?email=${encodeURIComponent(email.trim())}`;
      },
      error: () => {
        alert('No se pudo cargar la URL de conexión con Spotify.');
      }
    });
  }
}