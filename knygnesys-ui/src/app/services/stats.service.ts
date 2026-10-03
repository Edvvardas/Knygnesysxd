import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface StatsDataPoint {
  label: string;
  booksAdded: number;
  booksCompleted: number;
}

export interface StatsSummary {
  totalBooks: number;
  completed: number;
  reading: number;
  wantToRead: number;
}

export interface StatsResponse {
  summary: StatsSummary;
  period: string;
  dataPoints: StatsDataPoint[];
}

export interface PagesDayPoint {
  date: string;
  pagesRead: number;
}

export interface GenrePoint {
  genre: string;
  count: number;
}

@Injectable({ providedIn: 'root' })
export class StatsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:10032/api/stats';

  getStats(period: string, date?: string, startDate?: string, endDate?: string): Observable<StatsResponse> {
    const params: Record<string, string> = { period };
    if (date) params['date'] = date;
    if (startDate) params['startDate'] = startDate;
    if (endDate) params['endDate'] = endDate;
    return this.http.get<StatsResponse>(this.baseUrl, { params });
  }

  getPagesPerDay(days = 30, endDate?: string, startDate?: string): Observable<PagesDayPoint[]> {
    const params: Record<string, string | number> = { days };
    if (endDate) params['endDate'] = endDate;
    if (startDate) params['startDate'] = startDate;
    return this.http.get<PagesDayPoint[]>(`${this.baseUrl}/pages`, { params });
  }

  getGenreStats(): Observable<GenrePoint[]> {
    return this.http.get<GenrePoint[]>(`${this.baseUrl}/genres`);
  }
}
