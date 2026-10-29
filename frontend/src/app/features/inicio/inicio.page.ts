import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-inicio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <section class="flex flex-col items-center gap-6 py-10 text-center sm:py-16">
      <span
        class="rounded-full border border-borda bg-superficie/60 px-4 py-1.5 text-xs tracking-wide text-texto-suave"
      >A linha do tempo do seu grupo</span>

      <h1 class="max-w-2xl text-4xl font-semibold leading-tight sm:text-5xl">
        As fotos do grupo contam uma história.
        <span class="bg-gradient-to-r from-marca-suave to-destaque bg-clip-text text-transparent">
          Dê um nome a cada momento.
        </span>
      </h1>

      <p class="max-w-xl text-texto-suave">
        Reúna as fotos e vídeos de todo mundo em uma linha do tempo, agrupe por intervalo de
        data e hora, dê um título ao que aconteceu e comente junto.
      </p>

      <div class="flex flex-wrap items-center justify-center gap-3">
        <a
          routerLink="/grupos"
          class="rounded-full bg-marca px-5 py-2.5 text-sm font-medium text-fundo transition
                 hover:bg-marca-suave active:scale-95"
        >Ver meus grupos</a>
        <a
          routerLink="/descobrir"
          class="rounded-full border border-borda px-5 py-2.5 text-sm text-texto-suave transition
                 hover:border-marca hover:text-texto"
        >Descobrir grupos abertos</a>
      </div>
    </section>
  `,
})
export class InicioPage {}
