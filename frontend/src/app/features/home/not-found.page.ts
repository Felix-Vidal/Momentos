import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <section class="flex flex-col items-center gap-4 py-20 text-center">
      <p class="font-display text-6xl text-brand">404</p>
      <h1 class="text-2xl font-semibold">Essa página não existe</h1>
      <p class="max-w-sm text-muted">
        O link pode ter mudado ou o conteúdo é de um grupo do qual você não participa.
      </p>
      <a
        routerLink="/"
        class="rounded-full border border-line px-5 py-2.5 text-sm text-muted transition hover:border-brand hover:text-ink"
      >Voltar ao início</a>
    </section>
  `,
})
export class NotFoundPage {}
