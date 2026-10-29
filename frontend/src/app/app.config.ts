import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import {
  provideRouter,
  withComponentInputBinding,
  withInMemoryScrolling,
  withViewTransitions,
} from '@angular/router';
import { routes } from './app.routes';
import { errorInterceptor } from './core/api/error.interceptor';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AuthService } from './core/auth/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // A ordem importa: o de autenticação anexa o token e renova a sessão;
    // o de erro, por fora, traduz o que sobrar para o formato da API.
    provideHttpClient(withFetch(), withInterceptors([errorInterceptor, authInterceptor])),
    provideRouter(
      routes,
      withComponentInputBinding(),
      // A linha do tempo é longa: ao voltar, o scroll precisa cair onde estava.
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled', anchorScrolling: 'enabled' }),
      withViewTransitions(),
    ),
    // Retoma a sessão guardada antes de renderizar as rotas.
    provideAppInitializer(() => inject(AuthService).restore()),
  ],
};
