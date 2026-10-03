import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { GenresChartComponent } from './genres-chart.component';

vi.mock('chart.js', () => ({
  Chart: vi.fn().mockImplementation(() => ({ destroy: vi.fn() })),
}));

describe('GenresChartComponent', () => {
  let fixture: ComponentFixture<GenresChartComponent>;
  let component: GenresChartComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GenresChartComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(GenresChartComponent);
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

  it('export button is disabled when dataPoints is empty', () => {
    const btn: HTMLButtonElement = fixture.nativeElement.querySelector('.export-btn');
    expect(btn.disabled).toBe(true);
  });

  it('export button is enabled when dataPoints has entries', () => {
    // Set dataPoints before first detectChanges to avoid NG0100
    const localFixture = TestBed.createComponent(GenresChartComponent);
    localFixture.componentInstance.dataPoints = [{ genre: 'Fantastika', count: 5 }];
    localFixture.detectChanges();

    const btn: HTMLButtonElement = localFixture.nativeElement.querySelector('.export-btn');
    expect(btn.disabled).toBe(false);
  });

  it('exportPng() triggers download when dataPoints is populated', () => {
    // Set dataPoints before first detectChanges to avoid NG0100
    const localFixture = TestBed.createComponent(GenresChartComponent);
    localFixture.componentInstance.dataPoints = [{ genre: 'Fantastika', count: 3 }];
    localFixture.detectChanges();

    const canvas = localFixture.nativeElement.querySelector('canvas') as HTMLCanvasElement;
    const toDataURLSpy = vi.spyOn(canvas, 'toDataURL').mockReturnValue('data:image/png;base64,xyz');

    const mockLink = { href: '', download: '', click: vi.fn() };
    const origCreate = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tag: string) =>
      tag === 'a' ? (mockLink as unknown as HTMLElement) : origCreate(tag),
    );

    localFixture.componentInstance.exportPng();

    expect(toDataURLSpy).toHaveBeenCalledWith('image/png');
    expect(mockLink.download).toBe('zanrai.png');
    expect(mockLink.href).toContain('data:image/png');
    expect(mockLink.click).toHaveBeenCalledOnce();
  });

  it('exportPng() does nothing when dataPoints is empty (no canvas in DOM)', () => {
    // dataPoints = [] means @if branch renders empty-state div, not canvas; canvas() returns undefined
    const createSpy = vi.spyOn(document, 'createElement');
    component.exportPng();
    expect(createSpy).not.toHaveBeenCalledWith('a');
  });
});
