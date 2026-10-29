import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Protege rotas que exigem sessão, guardando o destino para voltar depois do login. */
export const authGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn()) {
    return true;
  }

  if (auth.refreshToken) {
    await auth.restore();
    if (auth.isLoggedIn()) {
      return true;
    }
  }

  return router.createUrlTree(['/entrar'], { queryParams: { retornar: state.url } });
};

/** Impede que quem já está logado veja as telas de entrada. */
export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn() && auth.refreshToken) {
    await auth.restore();
  }
  return auth.isLoggedIn() ? router.createUrlTree(['/grupos']) : true;
};
