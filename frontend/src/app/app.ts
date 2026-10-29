import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { ToastHost } from './shared/ui/toast-host';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, ToastHost],
  template: `
    <div class="flex min-h-dvh flex-col">
      <header class="vidro sticky top-0 z-50 border-b border-borda/60">
        <div class="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <a routerLink="/" class="group flex items-center gap-2">
            <span
              class="grid size-9 place-items-center rounded-full bg-gradient-to-br from-marca to-marca-forte
                     text-lg transition-transform duration-500 ease-[var(--ease-suave)] group-hover:rotate-180"
              aria-hidden="true"
            >◉</span>
            <span class="font-display text-lg font-semibold">Momentos</span>
          </a>
          <nav class="flex items-center gap-1 text-sm">
            <a
              routerLink="/grupos"
              class="rounded-full px-3 py-1.5 text-texto-suave transition hover:bg-superficie hover:text-texto"
            >Meus grupos</a>
            <a
              routerLink="/descobrir"
              class="rounded-full px-3 py-1.5 text-texto-suave transition hover:bg-superficie hover:text-texto"
            >Descobrir</a>
          </nav>
        </div>
      </header>

      <main class="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <router-outlet />
      </main>

      <footer class="border-t border-borda/60 px-4 py-6 text-center text-xs text-texto-fraco">
        Momentos · guarde as histórias do seu grupo
      </footer>
    </div>

    <app-toast-host />
  `,
})
export class App {}
