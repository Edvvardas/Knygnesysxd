import {inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Router} from '@angular/router';
import {catchError, map, of} from 'rxjs';

export const authGuard = () => {
  const http = inject(HttpClient);
  const router = inject(Router);

  return http.get('http://localhost:10032/api/users/me').pipe(
    map(() => true),
    catchError(() => {
      router.navigate(['/login']);
      return of(false);
    })
  );
};
