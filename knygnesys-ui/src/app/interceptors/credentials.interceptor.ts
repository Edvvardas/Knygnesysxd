import {HttpInterceptorFn} from '@angular/common/http';

export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith('http://localhost:10032')) {
    return next(req.clone({withCredentials: true}));
  }
  return next(req);
};
