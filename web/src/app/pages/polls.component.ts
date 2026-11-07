import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { PageResponse, Poll } from '../models';

@Component({
  selector: 'app-polls',
  standalone: true,
  imports: [FormsModule, DatePipe, DecimalPipe],
  template: `
    <h1>Sondages</h1>

    <form class="toolbar" (ngSubmit)="search()">
      <label>
        Institut
        <select name="institute" [(ngModel)]="institute">
          <option value="">Tous</option>
          @for (i of institutes; track i) {
            <option [value]="i">{{ i }}</option>
          }
        </select>
      </label>
      <label>
        Tour
        <select name="round" [(ngModel)]="round">
          <option [ngValue]="null">Tous</option>
          <option [ngValue]="1">Premier tour</option>
          <option [ngValue]="2">Second tour</option>
        </select>
      </label>
      <label>Du <input type="date" name="from" [(ngModel)]="from" /></label>
      <label>Au <input type="date" name="to" [(ngModel)]="to" /></label>
      <button type="submit">Filtrer</button>
    </form>

    @if (error) {
      <p class="error">Impossible de joindre l'API de consultation (port 8082).</p>
    } @else if (data) {
      <p class="muted">{{ data.page.totalElements }} résultat(s)</p>
      <table>
        <thead>
          <tr>
            <th>Fin de terrain</th>
            <th>Institut</th>
            <th>Commanditaire</th>
            <th>Échantillon</th>
            <th>Tour</th>
            <th>Hypothèse</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (p of data.content; track p.id) {
            <tr class="clickable" (click)="toggle(p.id)">
              <td>{{ p.fieldworkEnd ? (p.fieldworkEnd | date: 'dd/MM/yyyy') : '–' }}</td>
              <td>{{ p.institute }}</td>
              <td>{{ p.client || '–' }}</td>
              <td>{{ p.sampleSize ? (p.sampleSize | number: '1.0-0' : 'fr-FR') : '–' }}</td>
              <td>{{ p.round === 2 ? '2nd' : '1er' }}</td>
              <td>{{ p.scenario || '–' }}</td>
              <td class="chev">{{ expanded.has(p.id) ? '▾' : '▸' }}</td>
            </tr>
            @if (expanded.has(p.id)) {
              <tr class="detail">
                <td colspan="7">
                  @for (r of p.results; track r.candidate) {
                    <div class="bar-row">
                      <span class="name">{{ r.candidate }}</span>
                      <span class="bar"><span class="fill" [style.width.%]="Math.min(r.percentage * 2.5, 100)"></span></span>
                      <span class="value">{{ r.percentage | number: '1.1-1' : 'fr-FR' }} %</span>
                    </div>
                  }
                  @if (p.sourceUrl) {
                    <p><a [href]="p.sourceUrl" target="_blank" rel="noopener">Source</a></p>
                  }
                </td>
              </tr>
            }
          } @empty {
            <tr><td colspan="7" class="muted">Aucun sondage ne correspond à ces critères.</td></tr>
          }
        </tbody>
      </table>

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
export class PollsComponent implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly Math = Math;
  readonly size = 15;
  institutes: string[] = [];
  institute = '';
  round: number | null = null;
  from = '';
  to = '';
  page = 0;
  data?: PageResponse<Poll>;
  expanded = new Set<number>();
  error = false;

  ngOnInit(): void {
    this.api.institutes().subscribe({ next: (i) => (this.institutes = i), error: () => (this.error = true) });
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

  toggle(id: number): void {
    if (!this.expanded.delete(id)) {
      this.expanded.add(id);
    }
  }

  private load(): void {
    this.error = false;
    this.api
      .polls({
        institute: this.institute,
        round: this.round,
        from: this.from,
        to: this.to,
        page: this.page,
        size: this.size,
      })
      .subscribe({
        next: (d) => (this.data = d),
        error: () => (this.error = true),
      });
  }
}
