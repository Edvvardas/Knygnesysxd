import { Component, computed, inject, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { BookSearchService, BookSearchResult } from '../../services/book-search.service';
import { LibraryService, LibraryBook } from '../../services/library.service';
import { GoalService, GoalResponse } from '../../services/goal.service';

@Component({
  selector: 'app-home',
  imports: [CommonModule, FormsModule],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home implements OnDestroy {
  private readonly bookSearch = inject(BookSearchService);
  private readonly libraryService = inject(LibraryService);
  private readonly goalService = inject(GoalService);
  private readonly searchSubject = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  searchQuery = '';
  searchResults: BookSearchResult[] = [];
  isLoading = false;
  hasSearched = false;
  errorMessage = '';

  library = signal<LibraryBook[]>([]);
  addingBookKey = signal<string | null>(null);
  recommendations = signal<BookSearchResult[]>([]);
  recommendationsLoading = signal(false);
  goal = signal<GoalResponse | null>(null);
  settingGoal = signal(false);
  newGoalTarget = 10;

  statsTotal = computed(() => this.library().length);
  statsFinished = computed(() => this.library().filter(b => b.status === 'FINISHED').length);
  statsReading = computed(() => this.library().filter(b => b.status === 'READING').length);
  statsPages = computed(() => this.library().reduce((sum, b) => sum + (b.currentPage ?? 0), 0));

  constructor() {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        this.isLoading = true;
        this.hasSearched = true;
        this.errorMessage = '';
        this.searchResults = [];
        return this.bookSearch.search(query).pipe(
          catchError(() => {
            this.errorMessage = 'Nepavyko įkrauti rezultatų. Bandykite dar kartą.';
            return of([]);
          })
        );
      }),
      takeUntil(this.destroy$),
    ).subscribe(results => {
      this.searchResults = results;
      this.isLoading = false;
    });

    this.loadLibrary();
    this.loadGoal();
  }

  loadLibrary(): void {
    this.libraryService.getLibrary().subscribe({
      next: books => {
        this.library.set(books);
        this.loadRecommendations(books);
      },
    });
  }

  loadGoal(): void {
    this.goalService.getGoal().subscribe({
      next: goal => this.goal.set(goal),
    });
  }

  saveGoal(): void {
    if (this.newGoalTarget < 1) return;
    this.goalService.setGoal(this.newGoalTarget).subscribe({
      next: goal => {
        this.goal.set(goal);
        this.settingGoal.set(false);
      },
    });
  }

  private loadRecommendations(books: LibraryBook[]): void {
    const authors = [...new Set(
      books.map(b => b.author).filter((a): a is string => !!a && a.trim().length > 0)
    )];
    if (authors.length === 0) return;

    this.recommendationsLoading.set(true);
    const libraryKeys = new Set(books.map(b => b.olId));
    const unique = new Map<string, BookSearchResult>();
    let completed = 0;
    const toQuery = authors.slice(0, 3);

    for (const author of toQuery) {
      this.bookSearch.search(author, 10).pipe(
        catchError(() => of([] as BookSearchResult[])),
      ).subscribe(results => {
        for (const book of results) {
          if (!libraryKeys.has(book.key) && !unique.has(book.key)) {
            unique.set(book.key, book);
          }
        }
        completed++;
        if (completed === toQuery.length) {
          this.recommendations.set([...unique.values()].slice(0, 10));
          this.recommendationsLoading.set(false);
        }
      });
    }
  }

  addToLibrary(book: BookSearchResult): void {
    this.addingBookKey.set(book.key);
    this.libraryService.addBook({
      olId: book.key,
      title: book.title,
      author: book.authors[0] ?? null,
      coverUrl: book.coverUrl,
      pageCount: book.pageCount,
      status: 'WANT_TO_READ',
      genre: book.genre,
    }).subscribe({
      next: added => {
        this.library.update(lib => [...lib, added]);
        this.addingBookKey.set(null);
      },
      error: err => {
        console.error('Nepavyko pridėti knygos:', err);
        alert(err?.error?.message ?? err?.message ?? 'Nepavyko pridėti knygos.');
        this.addingBookKey.set(null);
      },
    });
  }

  isInLibrary(book: BookSearchResult): boolean {
    return this.library().some(b => b.olId === book.key);
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
    this.errorMessage = '';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
