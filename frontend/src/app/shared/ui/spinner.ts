import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-spinner',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex items-center justify-center gap-2 py-8 text-sm text-muted" role="status">
      <span
        class="size-4 animate-spin rounded-full border-2 border-line border-t-brand"
        aria-hidden="true"
      ></span>
      {{ label() }}
    </div>
  `,
})
export class Spinner {
  readonly label = input('Carregando…');
}
