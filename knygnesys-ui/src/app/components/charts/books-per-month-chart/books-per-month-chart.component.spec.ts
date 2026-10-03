import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { BooksPerMonthChartComponent } from './books-per-month-chart.component';

vi.mock('chart.js', () => ({
  Chart: vi.fn().mockImplementation(() => ({ destroy: vi.fn() })),
}));

describe('BooksPerMonthChartComponent', () => {
  let fixture: ComponentFixture<BooksPerMonthChartComponent>;
  let component: BooksPerMonthChartComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BooksPerMonthChartComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BooksPerMonthChartComponent);
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
    // Canvas is in the DOM after detectChanges; Chart.js is mocked so no timer needed
    const canvas = fixture.nativeElement.querySelector('canvas') as HTMLCanvasElement;
    const toDataURLSpy = vi.spyOn(canvas, 'toDataURL').mockReturnValue('data:image/png;base64,abc');

    const mockLink = { href: '', download: '', click: vi.fn() };
    const origCreate = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tag: string) =>
      tag === 'a' ? (mockLink as unknown as HTMLElement) : origCreate(tag),
    );

    component.exportPng();

    expect(toDataURLSpy).toHaveBeenCalledWith('image/png');
    expect(mockLink.download).toBe('knygos-per-menesi.png');
    expect(mockLink.href).toContain('data:image/png');
    expect(mockLink.click).toHaveBeenCalledOnce();
  });

});
