import { Component, Input, OnChanges } from '@angular/core';
import { CandidateTrend } from '../models';

interface Line {
  candidate: string;
  color: string;
  points: string;
  dots: { x: number; y: number; label: string }[];
}

const COLORS = ['#2563eb', '#dc2626', '#16a34a', '#d97706', '#7c3aed', '#0891b2'];

/** Courbes d'évolution des intentions de vote (SVG, sans dépendance externe). */
@Component({
  selector: 'app-trend-chart',
  standalone: true,
  template: `
    @if (lines.length) {
      <svg [attr.viewBox]="'0 0 ' + W + ' ' + H" role="img" aria-label="Évolution des intentions de vote">
        @for (t of yTicks; track t) {
          <line [attr.x1]="M.l" [attr.x2]="W - M.r" [attr.y1]="y(t)" [attr.y2]="y(t)" class="grid" />
          <text [attr.x]="M.l - 8" [attr.y]="y(t) + 4" text-anchor="end" class="tick">{{ t }} %</text>
        }
        @for (x of xTicks; track x.label) {
          <text [attr.x]="x.x" [attr.y]="H - 8" text-anchor="middle" class="tick">{{ x.label }}</text>
        }
        @for (l of lines; track l.candidate) {
          <polyline [attr.points]="l.points" fill="none" [attr.stroke]="l.color" stroke-width="2" />
          @for (p of l.dots; track $index) {
            <circle [attr.cx]="p.x" [attr.cy]="p.y" r="3.5" [attr.fill]="l.color">
              <title>{{ l.candidate }} – {{ p.label }}</title>
            </circle>
          }
        }
      </svg>
      <ul class="legend">
        @for (l of lines; track l.candidate) {
          <li><span class="dot" [style.background]="l.color"></span>{{ l.candidate }}</li>
        }
      </ul>
    } @else {
      <p class="muted">Pas assez de données pour tracer une courbe.</p>
    }
  `,
  styles: [
    `
      svg { width: 100%; height: auto; display: block; }
      .grid { stroke: #e5e7eb; stroke-width: 1; }
      .tick { fill: #6b7280; font-size: 11px; }
      .legend { list-style: none; display: flex; flex-wrap: wrap; gap: 6px 18px; padding: 0; margin: 12px 0 0; font-size: 14px; }
      .dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 6px; }
    `,
  ],
})
export class TrendChartComponent implements OnChanges {
  @Input() trends: CandidateTrend[] = [];
  @Input() maxCandidates = 6;

  readonly W = 760;
  readonly H = 340;
  readonly M = { l: 48, r: 16, t: 12, b: 32 };

  lines: Line[] = [];
  yTicks: number[] = [];
  xTicks: { x: number; label: string }[] = [];
  private yMax = 10;

  ngOnChanges(): void {
    const withPoints = this.trends.filter((t) => t.points.length > 0);
    const top = [...withPoints]
      .sort((a, b) => this.last(b) - this.last(a))
      .slice(0, this.maxCandidates);

    const times = top.flatMap((t) => t.points.map((p) => Date.parse(p.date)));
    if (!top.length || !times.length) {
      this.lines = [];
      return;
    }
    const tMin = Math.min(...times);
    const tMax = Math.max(...times);
    const span = Math.max(tMax - tMin, 1);
    const maxValue = Math.max(...top.flatMap((t) => t.points.map((p) => p.percentage)));
    this.yMax = Math.max(10, Math.ceil((maxValue + 2) / 5) * 5);
    this.yTicks = Array.from({ length: this.yMax / 5 + 1 }, (_, i) => i * 5);

    const x = (time: number) => this.M.l + ((time - tMin) / span) * (this.W - this.M.l - this.M.r);

    this.lines = top.map((t, i) => {
      const dots = t.points.map((p) => ({
        x: x(Date.parse(p.date)),
        y: this.y(p.percentage),
        label: `${p.percentage.toFixed(1)} % (${new Date(p.date).toLocaleDateString('fr-FR')})`,
      }));
      return {
        candidate: t.candidate,
        color: COLORS[i % COLORS.length],
        points: dots.map((d) => `${d.x.toFixed(1)},${d.y.toFixed(1)}`).join(' '),
        dots,
      };
    });

    this.xTicks = [0, 1, 2, 3].map((i) => {
      const time = tMin + (span * i) / 3;
      return {
        x: x(time),
        label: new Date(time).toLocaleDateString('fr-FR', { month: 'short', year: '2-digit' }),
      };
    });
  }

  y(value: number): number {
    return this.H - this.M.b - (value / this.yMax) * (this.H - this.M.t - this.M.b);
  }

  private last(t: CandidateTrend): number {
    return t.points[t.points.length - 1]?.percentage ?? 0;
  }
}
