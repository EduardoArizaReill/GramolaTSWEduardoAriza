
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SpotiService } from '../spoti-service';
import { PlayList } from '../model/PlayList';

@Component({
  selector: 'app-devices-and-playlists-component',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './devices-and-playlists-component.html',
  styleUrls: ['./devices-and-playlists-component.css']
})
export class DevicesAndPlaylistsComponent implements OnInit {

  devices: any[] = [];
  deviceError: string | null = null;

  playlists: PlayList[] = [];
  playlistError: string | null = null;

  currentPlaylist: any = null;
  currentlyPlaying: any = null;

  currentPlaylistError: string | null = null;
  currentlyPlayingError: string | null = null;

  constructor(private spoti: SpotiService) {}

  ngOnInit(): void {
    this.getDevices();
    this.getPlaylists();
    this.getCurrentPlayList();
  }

  /*
   * Cargo los dispositivos disponibles de Spotify.
   * Los necesito para saber desde dónde se está reproduciendo la música.
   */
  getDevices(): void {
    this.deviceError = null;

    this.spoti.getDevices().subscribe({
      next: (result: any) => {
        this.devices = result?.devices ?? [];
      },
      error: (err) => {
        this.devices = [];
        this.deviceError = err?.message ?? 'Error al cargar los dispositivos.';
      }
    });
  }

  /*
   * Cargo las playlists del usuario de Spotify.
   * Convierto cada elemento al modelo PlayList para trabajar de forma más ordenada.
   */
  getPlaylists(): void {
    this.playlistError = null;

    this.spoti.getPlaylists().subscribe({
      next: (result: any) => {
        const items = result?.items ?? [];
        this.playlists = items.map((item: any) => PlayList.fromJSON(item));
      },
      error: (err) => {
        this.playlists = [];
        this.playlistError = err?.message ?? 'Error al cargar las playlists.';
      }
    });
  }

  /*
   * Cargo la canción o reproducción actual.
   * En el servicio se llama getCurrentPlayList, aunque realmente devuelve
   * lo que Spotify tiene sonando ahora mismo.
   */
  getCurrentPlayList(): void {
    this.currentPlaylistError = null;
    this.currentlyPlayingError = null;

    this.spoti.getCurrentPlayList().subscribe({
      next: (response: any) => {
        this.currentPlaylist = response;
        this.currentlyPlaying = response;
      },
      error: (err) => {
        this.currentPlaylist = null;
        this.currentlyPlaying = null;

        const message = err?.message ?? 'Error al obtener la canción actual.';
        this.currentPlaylistError = message;
        this.currentlyPlayingError = message;
      }
    });
  }

  /*
   * Dejo este método por si el HTML antiguo lo llama.
   * Así no rompo nada, pero reutilizo el método correcto.
   */
  getCurrentlyPlaying(): void {
    this.getCurrentPlayList();
  }

  /*
   * Limpio todos los errores cuando quiera refrescar la información.
   */
  resetErrors(): void {
    this.deviceError = null;
    this.playlistError = null;
    this.currentPlaylistError = null;
    this.currentlyPlayingError = null;
  }
}