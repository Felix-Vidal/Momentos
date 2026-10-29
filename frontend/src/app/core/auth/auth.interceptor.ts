import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Rotas que nunca levam token nem disparam renovação. */
const PUBLIC_PATHS = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh'];

/**
 * Anexa o access token e, ao receber 401, tenta renovar a sessão uma única vez
 * e repetir a requisição — o usuário não percebe a troca de token.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  if (PUBLIC_PATHS.some((path) => req.url.startsWith(path))) {
    return next(req);
  }

  const first = withToken(req, auth.accessToken);

  return next(first).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || !auth.refreshToken) {
        return throwError(() => error);
      }
      return from(auth.renew()).pipe(
        switchMap((token) => next(withToken(req, token))),
        catchError((renewError) => {
          auth.clear();
          return throwError(() => renewError);
        }),
      );
    }),
  );
};

function withToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}
