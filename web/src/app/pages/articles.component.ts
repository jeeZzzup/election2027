import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { Article, PageResponse } from '../models';

@Component({
  selector: 'app-articles',
  standalone: true,
  imports: [FormsModule, DatePipe],
  template: `
    <h1>Actualités</h1>

    <form class="toolbar" (ngSubmit)="search()">
      <label class="grow">
        Recherche
        <input type="search" name="q" [(ngModel)]="q" placeholder="Candidat, institut, mot-clé…" />
      </label>
      <button type="submit">Rechercher</button>
    </form>

    @if (error) {
      <p class="error">Impossible de joindre l'API de consultation (port 8082).</p>
    } @else if (data) {
      <p class="muted">{{ data.page.totalElements }} article(s)</p>
      <ul class="articles">
        @for (a of data.content; track a.id) {
          <li>
            <a [href]="a.url" target="_blank" rel="noopener">{{ a.title }}</a>
            <div class="meta">
              {{ a.source || 'Source inconnue' }} · {{ a.publishedAt | date: 'dd/MM/yyyy HH:mm' }}
            </div>
            @if (a.summary) {
              <p>{{ a.summary }}</p>
            }
          </li>
        } @empty {
          <li class="muted">Aucun article.</li>
        }
      </ul>

      <div class="pager">
        <button type="button" (click)="go(-1)" [disabled]="data.page.number === 0">← Précédent</button>
        <span>Page {{ data.page.number + 1 }} / {{ Math.max(data.page.totalPages, 1) }}</span>
        <button type="button" (click)="go(1)" [disabled]="data.page.number + 1 >= data.page.totalPages">Suivant →</button>
      </div>
    } @else {
      <p class="muted">Chargement…</p>
    }
  `,
})
export class ArticlesComponent implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly Math = Math;
  readonly size = 15;
  q = '';
  page = 0;
  data?: PageResponse<Article>;
  error = false;

  ngOnInit(): void {
    this.load();
  }

  search(): void {
    this.page = 0;
    this.load();
  }

  go(delta: number): void {
    this.page += delta;
    this.load();
  }

  private load(): void {
    this.error = false;
    this.api.articles({ q: this.q, page: this.page, size: this.size }).subscribe({
      next: (d) => (this.data = d),
      error: () => (this.error = true),
    });
  }
}
