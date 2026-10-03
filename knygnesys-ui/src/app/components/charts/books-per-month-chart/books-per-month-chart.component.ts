import {
  Component,
  computed,
  Input,
  OnChanges,
  OnDestroy,
  signal,
  viewChild,
  ElementRef,
} from '@angular/core';
import { Chart } from 'chart.js';
import { StatsDataPoint } from '../../../services/stats.service';

const TITLES: Record<string, string> = {
  DAY: 'Knygos per dieną',
  WEEK: 'Knygos per savaitę',
  MONTH: 'Knygos per mėnesį',
  YEAR: 'Knygos per metus',
};

const SUBTITLES: Record<string, string> = {
  DAY: 'Pridėtos ir perskaitytos knygos per pasirinktą dieną',
  WEEK: 'Pridėtos ir perskaitytos knygos per pasirinktą savaitę',
  MONTH: 'Pridėtos ir perskaitytos knygos per pasirinktą mėnesį',
  YEAR: 'Pridėtos ir perskaitytos knygos per pasirinktus metus',
};

@Component({
  selector: 'app-books-per-month-chart',
  template: `
    <div class="chart-card">
      <div class="chart-header">
        <div class="chart-header-text">
          <h2 class="chart-title">{{ chartTitle() }}</h2>
          <p class="chart-subtitle">{{ chartSubtitle() }}</p>
        </div>
        <button class="export-btn" (click)="exportPng()">Eksportuoti PNG</button>
      </div>
      <div class="chart-body">
        <canvas #canvas></canvas>
      </div>
    </div>
  `,
  styleUrl: './books-per-month-chart.component.scss',
})
export class BooksPerMonthChartComponent implements OnChanges, OnDestroy {
  @Input() dataPoints: StatsDataPoint[] = [];
  @Input() period = 'YEAR';

  private _period = signal('YEAR');
  chartTitle = computed(() => TITLES[this._period()] ?? TITLES['YEAR']);
  chartSubtitle = computed(() => SUBTITLES[this._period()] ?? SUBTITLES['YEAR']);

  canvas = viewChild<ElementRef<HTMLCanvasElement>>('canvas');

  private chart: Chart | null = null;

  exportPng(): void {
    const el = this.canvas()?.nativeElement;
    if (!el) return;
    const link = document.createElement('a');
    link.download = 'knygos-per-menesi.png';
    link.href = el.toDataURL('image/png');
    link.click();
  }

  ngOnChanges(): void {
    this._period.set(this.period);
    setTimeout(() => this.render());
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private render(): void {
    const el = this.canvas()?.nativeElement;
    if (!el) return;

    this.chart?.destroy();

    this.chart = new Chart(el, {
      type: 'bar',
      data: {
        labels: this.dataPoints.map(dp => dp.label),
        datasets: [
          {
            label: 'Pridėta knygų',
            data: this.dataPoints.map(dp => dp.booksAdded),
            backgroundColor: 'rgba(59, 130, 246, 0.75)',
            borderColor: 'rgba(59, 130, 246, 1)',
            borderWidth: 1,
            borderRadius: 4,
          },
          {
            label: 'Perskaityta knygų',
            data: this.dataPoints.map(dp => dp.booksCompleted),
            backgroundColor: 'rgba(16, 185, 129, 0.75)',
            borderColor: 'rgba(16, 185, 129, 1)',
            borderWidth: 1,
            borderRadius: 4,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { boxWidth: 12, padding: 16 } },
          tooltip: { mode: 'index', intersect: false },
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: {
              maxTicksLimit: 12,
              maxRotation: 45,
              minRotation: 0,
              autoSkip: true,
            },
          },
          y: { beginAtZero: true, ticks: { stepSize: 1 }, grid: { color: '#f0f0f0' } },
        },
      },
    });
  }
}
