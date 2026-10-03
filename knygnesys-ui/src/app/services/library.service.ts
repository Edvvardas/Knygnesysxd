import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LibraryBook {
  readingListId: number;
  bookId: number;
  olId: string;
  title: string;
  author: string | null;
  coverUrl: string | null;
  pageCount: number | null;
  currentPage: number;
  status: string;
  addedAt: string | null;
  hasEpub: boolean;
}

export interface AddBookRequest {
  olId: string;
  title: string;
  author: string | null;
  coverUrl: string | null;
  pageCount: number | null;
  status: string;
  genre: string | null;
}

export interface LibraryFilter {
  title?: string;
  author?: string;
  progress?: number;
}

@Injectable({ providedIn: 'root' })
export class LibraryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:10032/api/library';

  getLibrary(): Observable<LibraryBook[]> {
    return this.http.get<LibraryBook[]>(this.baseUrl);
  }

  filterLibrary(filters: LibraryFilter): Observable<LibraryBook[]> {
    let params = new HttpParams();

    if (filters.title) {
      params = params.set('title', filters.title);
    }
    if (filters.author) {
      params = params.set('author', filters.author);
    }
    if (filters.progress !== undefined && filters.progress !== null) {
      params = params.set('progress', filters.progress.toString());
    }

    return this.http.get<LibraryBook[]>(`${this.baseUrl}/filter`, { params });
  }

  addBook(request: AddBookRequest): Observable<LibraryBook> {
    return this.http.post<LibraryBook>(this.baseUrl, request);
  }

  uploadEpub(readingListId: number, file: File): Observable<void> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<void>(`${this.baseUrl}/${readingListId}/epub`, formData);
  }

  getEpubUrl(readingListId: number): string {
    return `${this.baseUrl}/${readingListId}/epub`;
  }

  updateProgress(readingListId: number, currentPage: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${readingListId}/progress`, { currentPage });
  }

  removeBook(readingListId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${readingListId}`);
  }
}
