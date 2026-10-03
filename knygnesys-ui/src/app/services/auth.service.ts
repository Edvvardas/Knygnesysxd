import {inject, Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {tap} from 'rxjs/operators';

@Injectable({providedIn: 'root'})
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:10032/api/auth';

  currentUsername = signal<string | null>(localStorage.getItem('knygnesys-username'));

  register(email: string, username: string, password: string) {
    return this.http.post(`${this.apiUrl}/register`, {email, username, password});
  }

  login(username: string, password: string) {
    return this.http.post(`${this.apiUrl}/login`, {username, password}).pipe(
      tap(() => {
        this.currentUsername.set(username);
        localStorage.setItem('knygnesys-username', username);
      }),
    );
  }

  logout() {
    return this.http.post(`${this.apiUrl}/logout`, {}).pipe(
      tap(() => {
        this.currentUsername.set(null);
        localStorage.removeItem('knygnesys-username');
      }),
    );
  }
}
