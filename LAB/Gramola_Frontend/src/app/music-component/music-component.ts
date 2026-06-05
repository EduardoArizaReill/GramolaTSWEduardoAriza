/* CHECK
 * Componente principal de la pantalla de música.
 *
 * Desde esta pantalla gestiono la parte musical de la aplicación:
 * buscar canciones en Spotify, seleccionar una canción para pagarla,
 * guardar canciones pagadas en la cola del backend, consultar canciones
 * pendientes e historial, y enviar la siguiente canción pendiente a la cola
 * real de Spotify.
 *
 * También cargo dispositivos, canción actual y precio por canción.
 * Es uno de los componentes más importantes del frontend porque conecta
 * Spotify, pagos y la cola de canciones del backend.
 */
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { switchMap } from 'rxjs';

import { SpotiService } from '../spoti-service';
import { PaymentComponent } from '../payments-component/payments-componente';
import { AppUrlService } from '../app-url.service';

interface Track {
  name: string;
  uri: string;
  artists: { name: string }[];
  album?: {
    images?: { url: string }[];
  };
}

interface RequestedSong {
  id: string;
  title: string;
  artist: string;
  uri: string;
  image?: string;
  date: number;
  played: boolean;
  barEmail: string;
}

interface SongPlanResponse {
  amount: number;
  currency: string;
}

@Component({
  selector: 'app-music-component',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, PaymentComponent],
  templateUrl: './music-component.html',
  styleUrls: ['./music-component.css']
})
export class MusicComponent implements OnInit {

  searchTerm = '';
  searchResults: Track[] = [];

  playlists: any[] = [];
  devices: any[] = [];
  currentlyPlaying: any = null;

  selectedTrackToPay: Track | null = null;

  pendingQueue: RequestedSong[] = [];
  paidSongs: RequestedSong[] = [];

  queueMessage = '';
  currentSongMessage = '';

  pendingCount = 0;
  paidCount = 0;
  usedCount = 0;

  songAmountCents = 99;
  songCurrency = 'eur';

  constructor(
    private spoti: SpotiService,
    private http: HttpClient,
    private appUrlService: AppUrlService
  ) {}

  ngOnInit(): void {
    this.loadSongPlanPrice();
    this.loadDevices();
    this.loadPlaylists();
    this.loadCurrentlyPlaying();
    this.refreshQueueInfo();
  }

  private getBarEmail(): string {
    return (
      localStorage.getItem('email') ||
      sessionStorage.getItem('email') ||
      ''
    ).trim();
  }

  private getEncodedBarEmail(): string {
    return encodeURIComponent(this.getBarEmail());
  }

  private hasBarEmail(): boolean {
    return this.getBarEmail().length > 0;
  }

  /*
   * Cargo el precio por canción.
   * La URL de pagos sale de MySQL.
   */
  private loadSongPlanPrice(): void {
    const email = this.getBarEmail();

    this.appUrlService.getUrl('PAYMENTS_BASE_URL').pipe(
      switchMap((baseUrl) => {
        const url = email
          ? `${baseUrl}/songPlan?email=${encodeURIComponent(email)}`
          : `${baseUrl}/songPlan`;

        return this.http.get<SongPlanResponse>(
          url,
          { withCredentials: true }
        );
      })
    ).subscribe({
      next: (plan) => {
        this.songAmountCents = plan?.amount ?? this.songAmountCents;
        this.songCurrency = plan?.currency ?? this.songCurrency;
      },
      error: (err) => {
        console.warn('No se pudo cargar el precio de canción del bar.', err);
      }
    });
  }

  formatPrice(amountCents: number, currency: string): string {
    return `${(amountCents / 100).toFixed(2)} ${String(currency || 'eur').toUpperCase()}`;
  }

  formatDate(timestamp: number): string {
    return timestamp ? new Date(timestamp).toLocaleString('es-ES') : '';
  }

  loadDevices(): void {
    this.spoti.getDevices().subscribe({
      next: (data: any) => {
        this.devices = data?.devices ?? data ?? [];
        this.saveCurrentDeviceId();
      },
      error: (err) => {
        console.error('Error cargando dispositivos:', err);
      }
    });
  }

  private saveCurrentDeviceId(): void {
    const activeDevice = this.getActiveDevice();
    const deviceId = activeDevice?.id || this.devices[0]?.id;

    if (deviceId) {
      sessionStorage.setItem('currentDeviceId', deviceId);
    }
  }

  private getActiveDevice(): any {
    return this.devices.find((device) => device.is_active);
  }

  private getCurrentDeviceId(): string {
    return (
      this.getActiveDevice()?.id ||
      sessionStorage.getItem('currentDeviceId') ||
      ''
    );
  }

  loadPlaylists(): void {
    this.spoti.getPlaylists().subscribe({
      next: (data: any) => {
        this.playlists = data?.items ?? data ?? [];
      },
      error: (err) => {
        console.error('Error cargando playlists:', err);
      }
    });
  }

  loadCurrentlyPlaying(): void {
    this.currentSongMessage = 'Actualizando canción actual...';

    this.spoti.getCurrentPlayList().subscribe({
      next: (data: any) => {
        this.currentlyPlaying = data;

        this.currentSongMessage = data?.item
          ? 'Canción actualizada correctamente.'
          : 'Ahora mismo no hay ninguna canción sonando.';
      },
      error: (err) => {
        console.error('Error actualizando canción actual:', err);
        this.currentSongMessage = 'Error actualizando la canción actual.';
      }
    });
  }

  updateCurrentSong(): void {
    this.loadCurrentlyPlaying();
  }

  buscarCancion(): void {
    const query = this.searchTerm.trim();

    if (!query) {
      return;
    }

    this.searchResults = [];
    this.selectedTrackToPay = null;

    this.spoti.searchTracks(query).subscribe({
      next: (data: any) => {
        this.searchResults = data?.tracks?.items ?? [];
      },
      error: (err) => {
        console.error('Error buscando canción:', err);
        alert('Error buscando canción');
      }
    });
  }

  preparePayment(track: Track): void {
    this.selectedTrackToPay = track;
  }

  isSelectedTrack(track: Track): boolean {
    return this.selectedTrackToPay?.uri === track.uri;
  }

  cancelPayment(): void {
    this.selectedTrackToPay = null;
  }

  /*
   * Cuando el pago termina bien, guardo la canción en backend.
   * La URL de requestedSong sale de MySQL.
   */
  onPaymentSuccess(): void {
    const track = this.selectedTrackToPay;
    const barEmail = this.getBarEmail();

    if (!track) {
      alert('No hay canción seleccionada.');
      return;
    }

    if (!barEmail) {
      alert('No hay email del bar en sesión. No se puede guardar la canción.');
      return;
    }

    this.appUrlService.getUrl('REQUESTED_SONG_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.post<RequestedSong>(
          baseUrl,
          this.createSongPayload(track, barEmail),
          { withCredentials: true }
        )
      )
    ).subscribe({
      next: (savedSong) => {
        alert(`"${savedSong.title}" guardada en la cola del backend`);

        this.selectedTrackToPay = null;
        this.refreshQueueInfo();
        this.playNextFromBackend();
      },
      error: (err) => {
        console.error('Error guardando canción:', err);
        alert('Error guardando la canción en la cola');
      }
    });
  }

  private createSongPayload(track: Track, barEmail: string) {
    return {
      title: track.name,
      artist: track.artists?.[0]?.name || '',
      uri: track.uri,
      image: track.album?.images?.[0]?.url || null,
      date: Date.now(),
      played: false,
      barEmail
    };
  }

  refreshQueueInfo(): void {
    if (!this.hasBarEmail()) {
      this.clearQueueInfo('No hay email del bar en sesión.');
      return;
    }

    this.loadPendingQueue();
    this.loadPaidSongsHistory();
  }

  private clearQueueInfo(message: string): void {
    this.pendingQueue = [];
    this.paidSongs = [];

    this.pendingCount = 0;
    this.paidCount = 0;
    this.usedCount = 0;

    this.queueMessage = message;
  }

  loadPendingQueue(): void {
    if (!this.hasBarEmail()) {
      this.pendingQueue = [];
      this.pendingCount = 0;
      this.queueMessage = 'No hay email del bar en sesión.';
      return;
    }

    this.appUrlService.getUrl('REQUESTED_SONG_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.get<RequestedSong[]>(
          `${baseUrl}/queue/${this.getEncodedBarEmail()}`,
          { withCredentials: true }
        )
      )
    ).subscribe({
      next: (queue) => {
        this.pendingQueue = queue ?? [];
        this.pendingCount = this.pendingQueue.length;
        this.updateQueueMessage();
      },
      error: (err) => {
        console.error('Error cargando cola pendiente:', err);
        this.pendingQueue = [];
        this.pendingCount = 0;
        this.queueMessage = 'Error cargando la cola pendiente desde el backend.';
      }
    });
  }

  loadPaidSongsHistory(): void {
    if (!this.hasBarEmail()) {
      this.paidSongs = [];
      this.paidCount = 0;
      this.usedCount = 0;
      this.updateQueueMessage();
      return;
    }

    this.appUrlService.getUrl('REQUESTED_SONG_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.get<RequestedSong[]>(
          `${baseUrl}/history/${this.getEncodedBarEmail()}`,
          { withCredentials: true }
        )
      )
    ).subscribe({
      next: (songs) => {
        this.paidSongs = songs ?? [];
        this.paidCount = this.paidSongs.length;
        this.usedCount = this.paidSongs.filter((song) => song.played).length;
        this.updateQueueMessage();
      },
      error: (err) => {
        console.error('Error cargando historial:', err);
        this.paidSongs = [];
        this.paidCount = 0;
        this.usedCount = 0;
        this.updateQueueMessage();
      }
    });
  }

  private updateQueueMessage(): void {
    this.queueMessage =
      `Pendientes en backend: ${this.pendingCount}. ` +
      `Canciones pagadas por este bar: ${this.paidCount}. ` +
      `Ya enviadas/usadas: ${this.usedCount}.`;
  }

  playNextFromBackend(): void {
    if (!this.hasBarEmail()) {
      alert('No hay email del bar en sesión.');
      return;
    }

    this.appUrlService.getUrl('REQUESTED_SONG_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.get<RequestedSong>(
          `${baseUrl}/queue/${this.getEncodedBarEmail()}/next`,
          { withCredentials: true }
        )
      )
    ).subscribe({
      next: (song) => {
        this.playSongFromBackend(song);
      },
      error: (err) => {
        console.error('No hay siguiente canción:', err);
        this.refreshQueueInfo();
      }
    });
  }

  private playSongFromBackend(song: RequestedSong): void {
    const deviceId = this.getCurrentDeviceId();

    if (!deviceId) {
      alert(
        'No hay dispositivo activo en Spotify. ' +
        'Abre Spotify, reproduce una canción unos segundos y vuelve a intentarlo.'
      );

      this.refreshQueueInfo();
      return;
    }

    this.spoti.addSongToSpotifyQueue(song.uri, deviceId).subscribe({
      next: () => {
        this.markSongAsPlayed(song.id);
        alert(`Canción enviada a la cola de Spotify: ${song.title}`);
        this.loadCurrentlyPlaying();
      },
      error: (err) => {
        console.error('Error Spotify:', err);
        alert(this.getSpotifyErrorMessage(err));
        this.refreshQueueInfo();
      }
    });
  }

  private getSpotifyErrorMessage(err: any): string {
    if (err?.status === 404) {
      return 'Spotify no ha encontrado un reproductor activo. Abre Spotify, reproduce una canción y vuelve a intentarlo.';
    }

    if (err?.status === 401) {
      return 'El token de Spotify ha caducado. Vuelve a conectar con Spotify.';
    }

    if (err?.status === 403) {
      return 'Spotify no permite esta acción. Comprueba que la cuenta sea Premium.';
    }

    return 'No se pudo enviar la canción a Spotify.';
  }

  private markSongAsPlayed(songId: string): void {
    this.appUrlService.getUrl('REQUESTED_SONG_BASE_URL').pipe(
      switchMap((baseUrl) =>
        this.http.post<RequestedSong>(
          `${baseUrl}/${songId}/played`,
          {},
          { withCredentials: true }
        )
      )
    ).subscribe({
      next: () => {
        this.refreshQueueInfo();
      },
      error: (err) => {
        console.error('Error marcando canción como reproducida:', err);
        this.refreshQueueInfo();
      }
    });
  }
}