import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { ToastHost } from './shared/ui/toast-host';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastHost],
  template: `
    <div class="flex min-h-dvh flex-col">
      <header class="glass sticky top-0 z-50 border-b border-line/60">
        <div class="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <a routerLink="/" class="group flex items-center gap-2">
            <span
              class="grid size-9 place-items-center rounded-full bg-gradient-to-br from-brand to-brand-strong
                     text-lg transition-transform duration-500 ease-[var(--ease-soft)] group-hover:rotate-180"
              aria-hidden="true"
            >◉</span>
            <span class="font-display text-lg font-semibold">Momentos</span>
          </a>

          <nav class="flex items-center gap-1 text-sm">
            @if (isLoggedIn()) {
              <a
                routerLink="/grupos"
                routerLinkActive="bg-surface text-ink"
                class="rounded-full px-3 py-1.5 text-muted transition hover:bg-surface hover:text-ink"
              >Meus grupos</a>
              <a
                routerLink="/descobrir"
                routerLinkActive="bg-surface text-ink"
                class="rounded-full px-3 py-1.5 text-muted transition hover:bg-surface hover:text-ink"
              >Descobrir</a>
              <a
                routerLink="/perfil"
                routerLinkActive="ring-2 ring-brand"
                class="ml-1 grid size-9 place-items-center rounded-full bg-surface text-sm font-medium
                       text-brand-soft transition hover:bg-brand hover:text-base"
                [attr.aria-label]="'Perfil de ' + user()?.name"
                [title]="user()?.name ?? 'Meu perfil'"
              >{{ initials() }}</a>
            } @else {
              <a
                routerLink="/entrar"
                class="rounded-full px-3 py-1.5 text-muted transition hover:bg-surface hover:text-ink"
              >Entrar</a>
              <a
                routerLink="/criar-conta"
                class="rounded-full bg-brand px-4 py-1.5 font-medium text-base transition hover:bg-brand-soft"
              >Criar conta</a>
            }
          </nav>
        </div>
      </header>

      <main class="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <router-outlet />
      </main>

      <footer class="border-t border-line/60 px-4 py-6 text-center text-xs text-faint">
        Momentos · guarde as histórias do seu grupo
      </footer>
    </div>

    <app-toast-host />
  `,
})
export class App {
  private readonly auth = inject(AuthService);

  protected readonly user = this.auth.currentUser;
  protected readonly isLoggedIn = this.auth.isLoggedIn;

  protected initials(): string {
    const name = this.user()?.name ?? '';
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]!.toUpperCase())
      .join('');
  }
}
