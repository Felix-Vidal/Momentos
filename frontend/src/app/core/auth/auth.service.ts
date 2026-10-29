import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Credentials, Registration, Session, UserProfile } from './auth.models';

const REFRESH_KEY = 'momentos.refresh';

/**
 * Estado de sessão da aplicação.
 *
 * O access token fica apenas em memória (some ao recarregar a página) e o refresh token
 * em localStorage, para a sessão sobreviver a um F5. Ao iniciar, `restore()` troca o
 * refresh guardado por uma sessão nova.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly user = signal<UserProfile | null>(null);
  private readonly access = signal<string | null>(null);
  private readonly restoring = signal(false);

  readonly currentUser = this.user.asReadonly();
  readonly isLoggedIn = computed(() => this.user() !== null);
  readonly isRestoring = this.restoring.asReadonly();

  get accessToken(): string | null {
    return this.access();
  }

  get refreshToken(): string | null {
    try {
      return localStorage.getItem(REFRESH_KEY);
    } catch {
      return null;
    }
  }

  async register(data: Registration): Promise<void> {
    const session = await firstValueFrom(
      this.http.post<Session>('/api/auth/register', data),
    );
    this.apply(session);
  }

  async login(data: Credentials): Promise<void> {
    const session = await firstValueFrom(this.http.post<Session>('/api/auth/login', data));
    this.apply(session);
  }

  /** Retoma a sessão guardada ao abrir a aplicação. */
  async restore(): Promise<void> {
    const refreshToken = this.refreshToken;
    if (!refreshToken) {
      return;
    }
    this.restoring.set(true);
    try {
      await this.renew();
    } catch {
      this.clear();
    } finally {
      this.restoring.set(false);
    }
  }

  /** Troca o refresh token por uma sessão nova. Usado pelo interceptor ao receber 401. */
  async renew(): Promise<string> {
    const refreshToken = this.refreshToken;
    if (!refreshToken) {
      throw new Error('sem refresh token');
    }
    const session = await firstValueFrom(
      this.http.post<Session>('/api/auth/refresh', { refreshToken }),
    );
    this.apply(session);
    return session.accessToken;
  }

  async logout(redirect = true): Promise<void> {
    const refreshToken = this.refreshToken;
    if (refreshToken) {
      try {
        await firstValueFrom(this.http.post<void>('/api/auth/logout', { refreshToken }));
      } catch {
        // A sessão local é encerrada mesmo que o servidor não responda.
      }
    }
    this.clear();
    if (redirect) {
      await this.router.navigate(['/entrar']);
    }
  }

  patchUser(user: UserProfile): void {
    this.user.set(user);
  }

  clear(): void {
    this.user.set(null);
    this.access.set(null);
    try {
      localStorage.removeItem(REFRESH_KEY);
    } catch {
      // localStorage pode estar indisponível (janela privada); a sessão em memória já caiu.
    }
  }

  private apply(session: Session): void {
    this.user.set(session.user);
    this.access.set(session.accessToken);
    try {
      localStorage.setItem(REFRESH_KEY, session.refreshToken);
    } catch {
      // Sem persistência a sessão vale só nesta aba.
    }
  }
}
