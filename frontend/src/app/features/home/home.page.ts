import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <section class="flex flex-col items-center gap-6 py-10 text-center sm:py-16">
      <span class="rounded-full border border-line bg-surface/60 px-4 py-1.5 text-xs tracking-wide text-muted">
        A linha do tempo do seu grupo
      </span>

      <h1 class="max-w-2xl text-4xl leading-tight font-semibold sm:text-5xl">
        As fotos do grupo contam uma história.
        <span class="bg-gradient-to-r from-brand-soft to-accent bg-clip-text text-transparent">
          Dê um nome a cada momento.
        </span>
      </h1>

      <p class="max-w-xl text-muted">
        Reúna as fotos e vídeos de todo mundo em uma linha do tempo, agrupe por intervalo de
        data e hora, dê um título ao que aconteceu e comente junto.
      </p>

      <div class="flex flex-wrap items-center justify-center gap-3">
        <a
          routerLink="/grupos"
          class="rounded-full bg-brand px-5 py-2.5 text-sm font-medium text-base transition hover:bg-brand-soft active:scale-95"
        >Ver meus grupos</a>
        <a
          routerLink="/descobrir"
          class="rounded-full border border-line px-5 py-2.5 text-sm text-muted transition hover:border-brand hover:text-ink"
        >Descobrir grupos abertos</a>
      </div>
    </section>
  `,
})
export class HomePage {}
