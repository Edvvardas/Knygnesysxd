import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  viewChild,
  ElementRef,
} from '@angular/core';
import { Chart } from 'chart.js';
import { PagesDayPoint } from '../../../services/stats.service';

@Component({
  selector: 'app-pages-per-day-chart',
  template: `
    <div class="chart-card">
      <div class="chart-header">
        <div class="chart-header-text">
          <h2 class="chart-title">Puslapiai per dieną</h2>
          <p class="chart-subtitle">Perskaityti puslapiai per paskutines {{ dataPoints.length }} dienas</p>
        </div>
        <button class="export-btn" (click)="exportPng()">Eksportuoti PNG</button>
      </div>
      <div class="chart-body">
        <canvas #canvas></canvas>
      </div>
    </div>
  `,
  styleUrl: './pages-per-day-chart.component.scss',
})
export class PagesPerDayChartComponent implements OnChanges, OnDestroy {
  @Input() dataPoints: PagesDayPoint[] = [];

  canvas = viewChild<ElementRef<HTMLCanvasElement>>('canvas');

  private chart: Chart | null = null;

  exportPng(): void {
    const el = this.canvas()?.nativeElement;
    if (!el) return;
    const link = document.createElement('a');
    link.download = 'puslapiai-per-diena.png';
    link.href = el.toDataURL('image/png');
    link.click();
  }

  ngOnChanges(): void {
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
      type: 'line',
      data: {
        labels: this.dataPoints.map(dp => dp.date),
        datasets: [
          {
            label: 'Puslapiai',
            data: this.dataPoints.map(dp => dp.pagesRead),
            borderColor: 'rgba(139, 92, 246, 1)',
            backgroundColor: 'rgba(139, 92, 246, 0.08)',
            borderWidth: 2,
            pointRadius: 3,
            pointHoverRadius: 5,
            fill: true,
            tension: 0.35,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: ctx => ` ${ctx.parsed.y} puslapiai`,
            },
          },
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { maxTicksLimit: 8, maxRotation: 45, minRotation: 0, autoSkip: true },
          },
          y: {
            beginAtZero: true,
            ticks: { stepSize: 10 },
            grid: { color: '#f0f0f0' },
          },
        },
      },
    });
  }
}
