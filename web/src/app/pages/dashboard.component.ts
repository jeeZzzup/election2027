import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiService } from '../api.service';
import { TrendChartComponent } from '../components/trend-chart.component';
import { CandidateAverage, CandidateTrend, Stats } from '../models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, DatePipe, DecimalPipe, TrendChartComponent],
  template: `
    <h1>Tableau de bord</h1>

    @if (stats) {
      <section class="cards">
        <div class="card"><span class="big">{{ stats.polls }}</span><span>sondages</span></div>
        <div class="card"><span class="big">{{ stats.institutes }}</span><span>instituts</span></div>
        <div class="card"><span class="big">{{ stats.articles }}</span><span>articles</span></div>
        <div class="card">
          <span class="big">{{ stats.latestPollDate ? (stats.latestPollDate | date: 'dd/MM/yyyy') : '–' }}</span>
          <span>dernier terrain</span>
        </div>
      </section>
      @if (stats.lastCrawl) {
        <p class="muted">Dernière collecte réussie : {{ stats.lastCrawl | date: 'dd/MM/yyyy HH:mm' }}</p>
      }
    }

    <div class="toolbar">
      <label>
        Tour
        <select [ngModel]="round" (ngModelChange)="round = $event; load()">
          <option [ngValue]="1">Premier tour</option>
          <option [ngValue]="2">Second tour</option>
        </select>
      </label>
    </div>

    @if (error) {
      <p class="error">
        Impossible de joindre l'API de consultation. Vérifiez qu'elle tourne sur le port 8082.
      </p>
    } @else if (loading) {
      <p class="muted">Chargement…</p>
    } @else {
      <section class="panel">
        <h2>Moyenne des sondages récents</h2>
        <p class="muted">Moyenne des scores sur les 30 jours précédant le dernier sondage.</p>
        @for (a of averages; track a.candidate) {
          <div class="bar-row">
            <span class="name">{{ a.candidate }}</span>
            <span class="bar"><span class="fill" [style.width.%]="Math.min(a.percentage * 2.5, 100)"></span></span>
            <span class="value">{{ a.percentage | number: '1.1-1' : 'fr-FR' }} %</span>
          </div>
        } @empty {
          <p class="muted">Aucun sondage en base pour ce tour. Lancez une collecte (voir le README).</p>
        }
      </section>

      <section class="panel">
        <h2>Évolution</h2>
        <app-trend-chart [trends]="trends" />
      </section>
    }
  `,
})
export class DashboardComponent implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly Math = Math;
  stats?: Stats;
  averages: CandidateAverage[] = [];
  trends: CandidateTrend[] = [];
  round = 1;
  loading = true;
  error = false;

  ngOnInit(): void {
    this.api.stats().subscribe({ next: (s) => (this.stats = s), error: () => (this.error = true) });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = false;
    forkJoin([this.api.averages(this.round, 30), this.api.trends(this.round)]).subscribe({
      next: ([averages, trends]) => {
        this.averages = averages;
        this.trends = trends;
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      },
    });
  }
}
