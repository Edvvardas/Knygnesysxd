import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, Input } from '@angular/core';
import { of, Subject } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { StatsService, StatsResponse, PagesDayPoint, GenrePoint } from '../../services/stats.service';
import { BooksPerMonthChartComponent } from '../../components/charts/books-per-month-chart/books-per-month-chart.component';
import { GenresChartComponent } from '../../components/charts/genres-chart/genres-chart.component';
import { PagesPerDayChartComponent } from '../../components/charts/pages-per-day-chart/pages-per-day-chart.component';
import { StatistikaPuslapis } from './statistika.component';

@Component({
  selector: 'app-books-per-month-chart',
  standalone: true,
  template: '',
})
class BooksChartStubComponent {
  @Input() dataPoints: unknown[] = [];
  @Input() period = 'WEEK';
}

@Component({
  selector: 'app-pages-per-day-chart',
  standalone: true,
  template: '',
})
class PagesChartStubComponent {
  @Input() dataPoints: unknown[] = [];
}

@Component({
  selector: 'app-genres-chart',
  standalone: true,
  template: '',
})
class GenresChartStubComponent {
  @Input() dataPoints: unknown[] = [];
}

describe('StatistikaPuslapis', () => {
  let fixture: ComponentFixture<StatistikaPuslapis>;
  let component: StatistikaPuslapis;

  const statsResponse: StatsResponse = {
    period: 'WEEK',
    summary: { totalBooks: 1, completed: 0, reading: 1, wantToRead: 0 },
    dataPoints: [],
  };
  const pages: PagesDayPoint[] = [{ date: '2026-04-13', pagesRead: 10 }];
  const genres: GenrePoint[] = [{ genre: 'Fantasy', count: 1 }];

  const statsServiceMock = {
    getStats: vi.fn(() => of(statsResponse)),
    getPagesPerDay: vi.fn(() => of(pages)),
    getGenreStats: vi.fn(() => of(genres)),
  };

  beforeEach(async () => {
    statsServiceMock.getStats.mockReset();
    statsServiceMock.getPagesPerDay.mockReset();
    statsServiceMock.getGenreStats.mockReset();
    statsServiceMock.getStats.mockImplementation(() => of(statsResponse));
    statsServiceMock.getPagesPerDay.mockImplementation(() => of(pages));
    statsServiceMock.getGenreStats.mockImplementation(() => of(genres));

    await TestBed.configureTestingModule({
      imports: [
        StatistikaPuslapis,
        BooksChartStubComponent,
        PagesChartStubComponent,
        GenresChartStubComponent,
      ],
      providers: [{ provide: StatsService, useValue: statsServiceMock }],
    })
      .overrideComponent(StatistikaPuslapis, {
        remove: {
          imports: [BooksPerMonthChartComponent, PagesPerDayChartComponent, GenresChartComponent],
        },
        add: {
          imports: [BooksChartStubComponent, PagesChartStubComponent, GenresChartStubComponent],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(StatistikaPuslapis);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('uses selected week range for both stats and pages queries', () => {
    statsServiceMock.getStats.mockClear();
    statsServiceMock.getPagesPerDay.mockClear();

    component.applyPreset('LAST_7_DAYS');

    const statsArgs = statsServiceMock.getStats.mock.calls.at(-1) as unknown[] | undefined;
    const pagesArgs = statsServiceMock.getPagesPerDay.mock.calls.at(-1) as unknown[] | undefined;

    expect(statsArgs?.[0]).toBe('DAY');
    expect(statsArgs?.[2]).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(statsArgs?.[3]).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(pagesArgs?.[0]).toBe(7);
    expect(pagesArgs?.[1]).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(pagesArgs?.[2]).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('applies custom range only when user clicks apply', () => {
    statsServiceMock.getStats.mockClear();
    component.applyPreset('CUSTOM');
    component.onCustomStartChange({ target: { value: '2026-04-01' } } as unknown as Event);
    component.onCustomEndChange({ target: { value: '2026-04-10' } } as unknown as Event);

    expect(component.rangeStart()).not.toBe('2026-04-01');

    component.applyCustomRange();
    expect(component.rangeStart()).toBe('2026-04-01');
    expect(component.rangeEnd()).toBe('2026-04-10');
    expect(statsServiceMock.getStats).toHaveBeenCalled();
  });

  it('shows only the simplified preset set with Lithuanian labels', () => {
    const presetIds = component.presets.map(p => p.id);
    expect(presetIds).toEqual([
      'LAST_7_DAYS',
      'LAST_30_DAYS',
      'THIS_MONTH',
      'LAST_12_MONTHS',
      'CUSTOM',
    ]);

    const labels = component.presets.map(p => p.label);
    expect(labels).toContain('Pask. 7 dienos');
    expect(labels).toContain('Pask. 30 dienų');
    expect(labels).toContain('Šis mėnuo');
    expect(labels).toContain('Pask. 12 mėn.');
    expect(labels).toContain('Pasirinktinis');
  });

  it('hides custom controls when a preset is active', () => {
    component.applyPreset('LAST_7_DAYS');
    expect(component.isCustom()).toBe(false);
  });

  it('shows custom controls when CUSTOM preset is selected', () => {
    component.applyPreset('CUSTOM');
    expect(component.isCustom()).toBe(true);
  });

  it('ignores stale responses when newer period request finishes first', () => {
    const stats1 = new Subject<StatsResponse>();
    const stats2 = new Subject<StatsResponse>();
    const pages1 = new Subject<PagesDayPoint[]>();
    const pages2 = new Subject<PagesDayPoint[]>();
    const genres1 = new Subject<GenrePoint[]>();
    const genres2 = new Subject<GenrePoint[]>();

    let statsCall = 0;
    let pagesCall = 0;
    let genresCall = 0;

    statsServiceMock.getStats.mockImplementation(
      () => (statsCall++ === 0 ? stats1.asObservable() : stats2.asObservable()),
    );
    statsServiceMock.getPagesPerDay.mockImplementation(
      () => (pagesCall++ === 0 ? pages1.asObservable() : pages2.asObservable()),
    );
    statsServiceMock.getGenreStats.mockImplementation(
      () => (genresCall++ === 0 ? genres1.asObservable() : genres2.asObservable()),
    );

    fixture = TestBed.createComponent(StatistikaPuslapis);
    component = fixture.componentInstance;
    fixture.detectChanges(); // request #1

    component.prevPeriod(); // request #2

    stats2.next({
      period: 'WEEK',
      summary: { totalBooks: 2, completed: 1, reading: 1, wantToRead: 0 },
      dataPoints: [],
    });
    stats2.complete();
    pages2.next([{ date: '2026-04-13', pagesRead: 22 }]);
    pages2.complete();
    genres2.next([{ genre: 'Drama', count: 2 }]);
    genres2.complete();

    expect(component.stats()?.summary.totalBooks).toBe(2);
    expect(component.pagesPerDay()[0]?.pagesRead).toBe(22);

    stats1.next({
      period: 'WEEK',
      summary: { totalBooks: 99, completed: 99, reading: 0, wantToRead: 0 },
      dataPoints: [],
    });
    stats1.complete();
    pages1.next([{ date: '2026-04-13', pagesRead: 999 }]);
    pages1.complete();
    genres1.next([{ genre: 'Old', count: 99 }]);
    genres1.complete();

    expect(component.stats()?.summary.totalBooks).toBe(2);
    expect(component.pagesPerDay()[0]?.pagesRead).toBe(22);
  });
});
