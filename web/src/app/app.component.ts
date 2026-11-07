import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="top">
      <div class="wrap bar">
        <a class="brand" routerLink="/">Présidentielle 2027</a>
        <nav>
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Tableau de bord</a>
          <a routerLink="/sondages" routerLinkActive="active">Sondages</a>
          <a routerLink="/articles" routerLinkActive="active">Actualités</a>
        </nav>
      </div>
    </header>
    <main class="wrap">
      <router-outlet />
    </main>
    <footer class="wrap foot">
      Données collectées automatiquement, à titre informatif. Un sondage mesure un rapport de force à une
      date donnée : ce n'est pas une prévision du résultat.
    </footer>
  `,
})
export class AppComponent {}
