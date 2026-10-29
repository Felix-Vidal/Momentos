import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { authInterceptor } from './auth.interceptor';

import { AuthService } from './auth.service';

/** A renovação passa por uma Promise, então o retry só sai no tick seguinte. */
const tick = () => new Promise<void>((resolve) => setTimeout(resolve, 0));

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => backend.verify());

  it('não anexa token nas rotas públicas de autenticação', async () => {
    const done = firstValueFrom(http.post('/api/auth/login', {}));
    const req = backend.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
    await done;
  });

  it('ao receber 401, renova a sessão e repete a requisição com o token novo', async () => {
    localStorage.setItem('momentos.refresh', 'refresh-valido');

    const done = firstValueFrom(http.get('/api/groups'));

    // Primeira tentativa: sem token em memória, o servidor recusa.
    backend.expectOne('/api/groups').flush(
      { code: 'nao_autenticado', message: 'Faça login para continuar.' },
      { status: 401, statusText: 'Unauthorized' },
    );

    // O interceptor renova a sessão...
    backend.expectOne('/api/auth/refresh').flush({
      accessToken: 'token-novo',
      refreshToken: 'refresh-novo',
      expiresIn: 900,
      user: { id: 'u1', name: 'Felix' },
    });

    // ...e repete a chamada original já autenticada.
    await tick();
    const retry = backend.expectOne('/api/groups');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer token-novo');
    retry.flush([]);

    await done;
    expect(auth.accessToken).toBe('token-novo');
  });

  it('se a renovação falhar, a sessão é descartada', async () => {
    localStorage.setItem('momentos.refresh', 'refresh-expirado');

    const done = firstValueFrom(http.get('/api/groups')).catch((e) => e);

    backend.expectOne('/api/groups').flush(
      { code: 'nao_autenticado', message: 'Faça login para continuar.' },
      { status: 401, statusText: 'Unauthorized' },
    );
    backend.expectOne('/api/auth/refresh').flush(
      { code: 'sessao_invalida', message: 'Sua sessão expirou.' },
      { status: 401, statusText: 'Unauthorized' },
    );

    await done;
    await tick();
    expect(auth.isLoggedIn()).toBe(false);
    expect(localStorage.getItem('momentos.refresh')).toBeNull();
  });
});
