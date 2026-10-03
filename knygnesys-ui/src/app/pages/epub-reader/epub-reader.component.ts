import {
  Component,
  Input,
  Output,
  EventEmitter,
  AfterViewInit,
  OnDestroy,
  ElementRef,
  ViewChild,
  inject,
  NgZone,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { EpubStorageService } from '../../services/epub-storage.service';
import { LibraryService } from '../../services/library.service';
import { AuthService } from '../../services/auth.service';
import ePub from 'epubjs';

@Component({
  selector: 'app-epub-reader',
  imports: [CommonModule],
  templateUrl: './epub-reader.component.html',
  styleUrl: './epub-reader.component.scss',
})
export class EpubReaderComponent implements AfterViewInit, OnDestroy {
  @Input() bookKey!: string;
  @Input() title = '';
  @Input() readingListId!: number;
  @Input() pageCount: number | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() progressChanged = new EventEmitter<number>();

  @ViewChild('viewer') viewerRef!: ElementRef<HTMLDivElement>;

  private readonly epubStorage = inject(EpubStorageService);
  private readonly libraryService = inject(LibraryService);
  private readonly authService = inject(AuthService);
  private readonly zone = inject(NgZone);

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private book: any = null;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private rendition: any = null;
  private blobUrl: string | null = null;
  private keyHandler = this.onKeydown.bind(this);
  private static readonly SETTINGS_KEY_PREFIX = 'epub-reader-settings';
  private static readonly POSITION_KEY_PREFIX = 'epub-reader-position';
  private totalEpubPages = 0;

  isLoading = signal(true);
  error = signal('');
  theme = signal<'light' | 'dark'>(this.loadSettings().theme);
  fontSize = signal(this.loadSettings().fontSize);
  showSettings = signal(false);
  showToc = signal(false);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  toc = signal<any[]>([]);

  ngAfterViewInit(): void {
    document.addEventListener('keydown', this.keyHandler);
    this.loadEpub();
  }

  private async loadEpub(): Promise<void> {
    try {
      console.log('[EPUB] 1. Loading file from IndexedDB, key:', this.bookKey);
      const file = await this.epubStorage.load(this.bookKey);
      console.log('[EPUB] 2. File loaded:', file ? `${(file as Blob).size} bytes` : 'NULL');

      if (!file) {
        this.zone.run(() => {
          this.error.set('EPUB failas nerastas. Bandykite prisegti failą iš naujo.');
          this.isLoading.set(false);
        });
        return;
      }

      console.log('[EPUB] 3. Converting to ArrayBuffer...');
      const arrayBuffer = await (file as Blob).arrayBuffer();
      console.log('[EPUB] 4. ArrayBuffer size:', arrayBuffer.byteLength);

      console.log('[EPUB] 5. Creating ePub book...');
      this.book = ePub(arrayBuffer);

      console.log('[EPUB] 6. Waiting for book.ready...');
      await this.book.ready;
      console.log('[EPUB] 7. Book ready!');

      const el = this.viewerRef.nativeElement;
      console.log('[EPUB] 8. Viewer element:', el.tagName, 'size:', el.clientWidth, 'x', el.clientHeight);

      this.rendition = this.book.renderTo(el, {
        width: el.clientWidth || 600,
        height: el.clientHeight || 500,
      });
      console.log('[EPUB] 9. Rendition created, calling display()...');

      const savedCfi = this.loadPosition();
      await this.rendition.display(savedCfi || undefined);
      console.log('[EPUB] 10. Display complete!');

      this.applyTheme(this.theme());
      this.applyFontSize(this.fontSize());

      await this.book.locations.generate(1024);
      this.totalEpubPages = this.book.locations.length();

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      this.rendition.on('relocated', (location: any) => {
        const cfi = location?.start?.cfi;
        if (cfi) {
          this.savePosition(cfi);
          this.syncProgress(location);
        }
      });

      const navigation = await this.book.loaded.navigation;
      this.zone.run(() => {
        this.toc.set(navigation.toc || []);
        this.isLoading.set(false);
      });
    } catch (err) {
      console.error('[EPUB] ERROR at some step:', err);
      this.zone.run(() => {
        this.error.set('Nepavyko atidaryti EPUB failo.');
        this.isLoading.set(false);
      });
    }
  }

  prevPage(): void {
    this.rendition?.prev();
  }

  nextPage(): void {
    this.rendition?.next();
  }

  toggleSettings(): void {
    this.showSettings.update(v => !v);
    if (this.showSettings()) this.showToc.set(false);
  }

  toggleToc(): void {
    this.showToc.update(v => !v);
    if (this.showToc()) this.showSettings.set(false);
  }

  goToChapter(href: string): void {
    this.rendition?.display(href);
    this.showToc.set(false);
  }

  setTheme(theme: 'light' | 'dark'): void {
    this.theme.set(theme);
    this.applyTheme(theme);
    this.saveSettings();
  }

  changeFontSize(delta: number): void {
    const next = Math.min(180, Math.max(60, this.fontSize() + delta));
    this.fontSize.set(next);
    this.applyFontSize(next);
    this.saveSettings();
  }

  private applyTheme(theme: 'light' | 'dark'): void {
    if (!this.rendition) return;
    if (theme === 'dark') {
      this.rendition.themes.override('color', '#ccc');
      this.rendition.themes.override('background', '#1a1a2e');
    } else {
      this.rendition.themes.override('color', '#000');
      this.rendition.themes.override('background', '#fff');
    }
  }

  private applyFontSize(size: number): void {
    if (!this.rendition) return;
    this.rendition.themes.fontSize(`${size}%`);
  }

  private onKeydown(e: KeyboardEvent): void {
    if (e.key === 'ArrowLeft') this.prevPage();
    else if (e.key === 'ArrowRight') this.nextPage();
    else if (e.key === 'Escape') this.zone.run(() => this.onClose());
  }

  private get userPrefix(): string {
    return this.authService.currentUsername() ?? 'default';
  }

  private get settingsKey(): string {
    return `${EpubReaderComponent.SETTINGS_KEY_PREFIX}-${this.userPrefix}`;
  }

  private get positionKey(): string {
    return `${EpubReaderComponent.POSITION_KEY_PREFIX}-${this.userPrefix}-${this.bookKey}`;
  }

  private loadSettings(): { theme: 'light' | 'dark'; fontSize: number } {
    try {
      const raw = localStorage.getItem(this.settingsKey);
      if (raw) return JSON.parse(raw);
    } catch { /* ignore */ }
    return { theme: 'light', fontSize: 100 };
  }

  private saveSettings(): void {
    localStorage.setItem(
      this.settingsKey,
      JSON.stringify({ theme: this.theme(), fontSize: this.fontSize() }),
    );
  }

  private loadPosition(): string | null {
    try {
      return localStorage.getItem(this.positionKey);
    } catch { return null; }
  }

  private savePosition(cfi: string): void {
    localStorage.setItem(this.positionKey, cfi);
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private syncProgress(location: any): void {
    if (this.totalEpubPages === 0) return;
    const epubPage = location.start?.location ?? 0;
    const realPage = this.pageCount
      ? Math.round((epubPage / this.totalEpubPages) * this.pageCount)
      : epubPage;
    this.progressChanged.emit(realPage);
    this.libraryService.updateProgress(this.readingListId, realPage).subscribe();
  }

  onClose(): void {
    this.close.emit();
  }

  ngOnDestroy(): void {
    document.removeEventListener('keydown', this.keyHandler);
    this.rendition?.destroy();
    this.book?.destroy();
    if (this.blobUrl) URL.revokeObjectURL(this.blobUrl);
  }
}
