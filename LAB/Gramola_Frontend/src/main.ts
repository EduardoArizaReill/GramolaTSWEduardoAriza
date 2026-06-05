// src/main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { routes } from './app/app.routes';
import { provideHttpClient } from '@angular/common/http';
import { Component } from '@angular/core';

@Component({
selector: 'app-root',
standalone: true,
imports: [RouterOutlet, RouterLink, RouterLinkActive],
template: `
<header class="topbar">
<div class="brand">
<span class="logo">🎵</span>
<span class="title">Mi Gramola</span>
</div>

<nav class="menu">
<a routerLink="/login" routerLinkActive="active">Login</a>
<a routerLink="/register" routerLinkActive="active">Registrar</a>
</nav>
</header>

<!-- Aquí se cargarán tus páginas -->
<main class="content">
<router-outlet></router-outlet>
</main>
`,
styles: [`
.topbar {
position: sticky;
top: 0;
z-index: 10;
display: flex;
align-items: center;
justify-content: space-between;
padding: 12px 20px;
background: #0f172a;
color: #fff;
box-shadow: 0 2px 8px rgba(0,0,0,.15);
}
.brand { display: flex; align-items: center; gap: 8px; }
.logo { font-size: 20px; }
.title { font-weight: 600; }
.menu a { color: #cbd5e1; text-decoration: none; margin-left: 16px; font-weight: 500; }
.menu a:hover, .menu a.active { color: #fff; }

.content { padding: 16px; }
`]
})
class LanzadoraComponent {


}

bootstrapApplication(LanzadoraComponent, {
providers: [
provideRouter(routes),
provideHttpClient()
]
})