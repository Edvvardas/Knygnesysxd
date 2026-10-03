import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { PagesPerDayChartComponent } from './pages-per-day-chart.component';

vi.mock('chart.js', () => ({
  Chart: vi.fn().mockImplementation(() => ({ destroy: vi.fn() })),
}));

describe('PagesPerDayChartComponent', () => {
  let fixture: ComponentFixture<PagesPerDayChartComponent>;
  let component: PagesPerDayChartComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PagesPerDayChartComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PagesPerDayChartComponent);
    component = fixture.componentInstance;
    component.dataPoints = [];
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the export button with correct label', () => {
    const btn: HTMLButtonElement = fixture.nativeElement.querySelector('.export-btn');
    expect(btn).toBeTruthy();
    expect(btn.textContent?.trim()).toBe('Eksportuoti PNG');
  });

  it('export button is not disabled', () => {
    const btn: HTMLButtonElement = fixture.nativeElement.querySelector('.export-btn');
    expect(btn.disabled).toBe(false);
  });

  it('exportPng() calls toDataURL and triggers a download', () => {
    // Set dataPoints before first detectChanges to avoid NG0100
    const localFixture = TestBed.createComponent(PagesPerDayChartComponent);
    localFixture.componentInstance.dataPoints = [{ date: '2024-04-01', pagesRead: 12 }];
    localFixture.detectChanges();

    const canvas = localFixture.nativeElement.querySelector('canvas') as HTMLCanvasElement;
    const toDataURLSpy = vi.spyOn(canvas, 'toDataURL').mockReturnValue('data:image/png;base64,def');

    const mockLink = { href: '', download: '', click: vi.fn() };
    const origCreate = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tag: string) =>
      tag === 'a' ? (mockLink as unknown as HTMLElement) : origCreate(tag),
    );

    localFixture.componentInstance.exportPng();

    expect(toDataURLSpy).toHaveBeenCalledWith('image/png');
    expect(mockLink.download).toBe('puslapiai-per-diena.png');
    expect(mockLink.href).toContain('data:image/png');
    expect(mockLink.click).toHaveBeenCalledOnce();
  });

});
