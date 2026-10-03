import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface GoalResponse {
  targetBooks: number | null;
  booksRead: number;
  progressPercent: number;
}

@Injectable({ providedIn: 'root' })
export class GoalService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:10032/api/goals';

  getGoal(): Observable<GoalResponse> {
    return this.http.get<GoalResponse>(this.baseUrl);
  }

  setGoal(targetBooks: number): Observable<GoalResponse> {
    return this.http.post<GoalResponse>(this.baseUrl, { targetBooks });
  }
}
