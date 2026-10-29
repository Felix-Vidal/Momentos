import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { Session } from './auth.models';

const session: Session = {
  accessToken: 'token-de-acesso',
  refreshToken: 'token-de-renovacao',
  expiresIn: 900,
  user: { id: 'u1', name: 'Felix', email: 'felix@ufac.br' },
};

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('começa sem sessão', () => {
    expect(service.isLoggedIn()).toBe(false);
    expect(service.accessToken).toBeNull();
  });

  it('login guarda o usuário em memória e o refresh token no localStorage', async () => {
    const done = service.login({ email: 'felix@ufac.br', password: 'senhaSegura123' });
    http.expectOne('/api/auth/login').flush(session);
    await done;

    expect(service.isLoggedIn()).toBe(true);
    expect(service.currentUser()?.name).toBe('Felix');
    expect(service.accessToken).toBe('token-de-acesso');
    expect(localStorage.getItem('momentos.refresh')).toBe('token-de-renovacao');
  });

  it('renew troca o refresh token guardado pelo novo', async () => {
    localStorage.setItem('momentos.refresh', 'token-antigo');

    const done = service.renew();
    const req = http.expectOne('/api/auth/refresh');
    expect(req.request.body).toEqual({ refreshToken: 'token-antigo' });
    req.flush(session);
    await done;

    expect(localStorage.getItem('momentos.refresh')).toBe('token-de-renovacao');
  });

  it('restore sem refresh token guardado não chama a API', async () => {
    await service.restore();
    expect(service.isLoggedIn()).toBe(false);
  });

  it('restore descarta a sessão quando o refresh token não vale mais', async () => {
    localStorage.setItem('momentos.refresh', 'token-expirado');

    const done = service.restore();
    http.expectOne('/api/auth/refresh').flush(
      { code: 'sessao_invalida', message: 'Sua sessão expirou. Entre novamente.' },
      { status: 401, statusText: 'Unauthorized' },
    );
    await done;

    expect(service.isLoggedIn()).toBe(false);
    expect(localStorage.getItem('momentos.refresh')).toBeNull();
  });

  it('logout revoga no servidor e limpa o estado local', async () => {
    localStorage.setItem('momentos.refresh', 'token-de-renovacao');

    const done = service.logout(false);
    http.expectOne('/api/auth/logout').flush(null);
    await done;

    expect(service.isLoggedIn()).toBe(false);
    expect(localStorage.getItem('momentos.refresh')).toBeNull();
  });
});
