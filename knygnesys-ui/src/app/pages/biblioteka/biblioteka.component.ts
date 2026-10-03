import { Component, inject, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { BookSearchService, BookSearchResult } from '../../services/book-search.service';
import { LibraryService, LibraryBook, LibraryFilter } from '../../services/library.service';
import { EpubStorageService } from '../../services/epub-storage.service';
import { EpubReaderComponent } from '../epub-reader/epub-reader.component';

@Component({
  selector: 'app-biblioteka',
  imports: [CommonModule, FormsModule, EpubReaderComponent],
  templateUrl: './biblioteka.component.html',
  styleUrl: './biblioteka.component.scss',
})
export class BibliotekaPuslapis implements OnDestroy {
  private readonly bookSearch = inject(BookSearchService);
  private readonly libraryService = inject(LibraryService);
  private readonly epubStorage = inject(EpubStorageService);
  private readonly searchSubject = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  searchQuery = '';
  searchResults: BookSearchResult[] = [];
  isSearchLoading = false;
  hasSearched = false;
  searchError = '';

  library = signal<LibraryBook[]>([]);
  filters = signal<LibraryFilter>({
    title: '',
    author: '',
    progress: undefined
  });
  isLibraryLoading = signal(false);
  libraryError = signal('');

  addingBookKey = signal<string | null>(null);
  uploadingKey = signal<string | null>(null);

  // Keys of books that have EPUB stored locally
  localEpubKeys = signal<Set<string>>(new Set());

  epubReaderKey = signal<string | null>(null);
  epubReaderTitle = signal('');
  epubReaderListId = signal<number | null>(null);
  epubReaderPageCount = signal<number | null>(null);

  activeFilter = signal<string>('VISI');

  constructor() {
    this.searchSubject
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(query => {
          this.isSearchLoading = true;
          this.hasSearched = true;
          this.searchError = '';
          this.searchResults = [];
          return this.bookSearch.search(query).pipe(
            catchError(() => {
              this.searchError = 'Nepavyko įkrauti rezultatų. Bandykite dar kartą.';
              return of([]);
            }),
          );
        }),
        takeUntil(this.destroy$),
      )
      .subscribe(results => {
        this.searchResults = results;
        this.isSearchLoading = false;
      });

    this.loadLibrary();
    this.loadLocalEpubKeys();
  }

  applyFilters(): void {
    const filterRequest: LibraryFilter = {};

    if (this.filters().title?.trim()) {
      filterRequest.title = this.filters().title;
    }
    if (this.filters().author?.trim()) {
      filterRequest.author = this.filters().author;
    }
    if (this.filters().progress !== undefined && this.filters().progress !== null) {
      filterRequest.progress = this.filters().progress;
    }

    this.libraryService.filterLibrary(filterRequest).subscribe({
      next: (library) => {
        this.library.set(library);
      },
      error: (error) => {
        console.error('Error filtering library:', error);
      }
    });
  }

  clearFilters(): void {
    this.filters.set({
      title: '',
      author: '',
      progress: undefined
    });
    this.loadLibrary();
  }

  private async loadLocalEpubKeys(): Promise<void> {
    const keys = await this.epubStorage.getAllKeys();
    this.localEpubKeys.set(new Set(keys));
  }

  onSearch(): void {
    const query = this.searchQuery.trim();
    if (!query) return;
    this.searchSubject.next(query);
  }

  onSearchKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') this.onSearch();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchResults = [];
    this.hasSearched = false;
    this.searchError = '';
  }

  loadLibrary(): void {
    this.isLibraryLoading.set(true);
    this.libraryService.getLibrary().subscribe({
      next: books => {
        this.library.set(books);
        this.isLibraryLoading.set(false);
      },
      error: () => {
        this.libraryError.set('Nepavyko įkrauti bibliotekos.');
        this.isLibraryLoading.set(false);
      },
    });
  }

  addToLibrary(book: BookSearchResult): void {
    this.addingBookKey.set(book.key);
    this.libraryService
      .addBook({
        olId: book.key,
        title: book.title,
        author: book.authors[0] ?? null,
        coverUrl: book.coverUrl,
        pageCount: book.pageCount,
        status: 'WANT_TO_READ',
        genre: book.genre,
      })
      .subscribe({
        next: added => {
          this.library.update(lib => [...lib, added]);
          this.addingBookKey.set(null);
        },
        error: err => {
          const msg = err?.error?.message ?? 'Nepavyko pridėti knygos.';
          alert(msg);
          this.addingBookKey.set(null);
        },
      });
  }

  isInLibrary(book: BookSearchResult): boolean {
    return this.library().some(b => b.olId === book.key);
  }

  hasLocalEpub(book: LibraryBook): boolean {
    return this.localEpubKeys().has(book.olId);
  }

  async onEpubFileSelected(event: Event, book: LibraryBook): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploadingKey.set(book.olId);
    try {
      await this.epubStorage.save(book.olId, file);
      this.localEpubKeys.update(keys => new Set([...keys, book.olId]));
    } catch {
      alert('Nepavyko išsaugoti EPUB failo.');
    } finally {
      this.uploadingKey.set(null);
      input.value = '';
    }
  }

  openEpubReader(book: LibraryBook): void {
    this.epubReaderKey.set(book.olId);
    this.epubReaderTitle.set(book.title);
    this.epubReaderListId.set(book.readingListId);
    this.epubReaderPageCount.set(book.pageCount);
  }

  closeEpubReader(): void {
    this.epubReaderKey.set(null);
    this.epubReaderTitle.set('');
    this.epubReaderListId.set(null);
    this.epubReaderPageCount.set(null);
    this.loadLibrary();
  }

  onProgressChanged(currentPage: number): void {
    const key = this.epubReaderKey();
    if (!key) return;
    this.library.update(lib =>
      lib.map(b => {
        if (b.olId !== key) return b;
        return { ...b, currentPage, status: this.resolveStatus(b, currentPage) };
      }),
    );
  }

  changeCurrentPage(book: LibraryBook, delta: number): void {
    const upper = book.pageCount ?? Infinity;
    const next = Math.max(0, Math.min(upper, book.currentPage + delta));
    if (next === book.currentPage) return;
    this.updatePage(book, next);
  }

  setCurrentPage(book: LibraryBook, event: Event): void {
    const value = parseInt((event.target as HTMLInputElement).value, 10);
    if (isNaN(value)) return;
    const upper = book.pageCount ?? Infinity;
    const next = Math.max(0, Math.min(upper, value));
    this.updatePage(book, next);
  }

  private updatePage(book: LibraryBook, next: number): void {
    const status = this.resolveStatus(book, next);
    this.library.update(lib =>
      lib.map(b =>
        b.readingListId === book.readingListId ? { ...b, currentPage: next, status } : b,
      ),
    );
    this.libraryService.updateProgress(book.readingListId, next).subscribe();
  }

  private resolveStatus(book: LibraryBook, page: number): string {
    if (book.pageCount && page >= book.pageCount) return 'FINISHED';
    if (page > 0) return book.status === 'WANT_TO_READ' ? 'READING' : book.status;
    return book.status;
  }

  removeFromLibrary(book: LibraryBook): void {
    if (!confirm(`Pašalinti „${book.title}" iš bibliotekos?`)) return;
    this.libraryService.removeBook(book.readingListId).subscribe({
      next: async () => {
        this.library.update(lib => lib.filter(b => b.readingListId !== book.readingListId));
        await this.epubStorage.remove(book.olId);
        this.localEpubKeys.update(keys => {
          const next = new Set(keys);
          next.delete(book.olId);
          return next;
        });
      },
      error: () => alert('Nepavyko pašalinti knygos.'),
    });
  }

  countByStatus(status: string): number {
    return this.library().filter(b => b.status === status).length;
  }

  get filteredLibrary(): LibraryBook[] {
    const filter = this.activeFilter();
    if (filter === 'VISI') return this.library();
    return this.library().filter(b => b.status === filter);
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      READING: 'Skaitau',
      WANT_TO_READ: 'Noriu skaityti',
      FINISHED: 'Perskaičiau',
    };
    return labels[status] ?? status;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
