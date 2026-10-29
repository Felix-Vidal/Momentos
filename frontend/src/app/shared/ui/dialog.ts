import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Janela modal com fundo desfocado. Fecha no Esc, no clique fora e no botão de fechar.
 */
@Component({
  selector: 'app-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="fixed inset-0 z-90 flex items-end justify-center bg-black/70 p-0 backdrop-blur-sm
             sm:items-center sm:p-6 animate-[fade_.2s_ease-out]"
      (click)="closed.emit()"
    >
      <div
        class="glass flex max-h-[92dvh] w-full max-w-2xl flex-col overflow-hidden rounded-t-box
               sm:rounded-box animate-[lift_.3s_var(--ease-soft)]"
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="title()"
        (click)="$event.stopPropagation()"
      >
        <header class="flex items-center justify-between gap-4 border-b border-line/60 px-5 py-3.5">
          <h2 class="font-display text-lg">{{ title() }}</h2>
          <button
            type="button"
            class="grid size-8 place-items-center rounded-full text-muted transition
                   hover:bg-surface hover:text-ink"
            (click)="closed.emit()"
            aria-label="Fechar"
          >×</button>
        </header>

        <div class="flex-1 overflow-y-auto px-5 py-4">
          <ng-content />
        </div>

        <footer class="flex items-center justify-end gap-2 border-t border-line/60 px-5 py-3.5">
          <ng-content select="[slot=actions]" />
        </footer>
      </div>
    </div>
  `,
  styles: `
    @keyframes fade { from { opacity: 0; } to { opacity: 1; } }
    @keyframes lift {
      from { opacity: 0; transform: translateY(24px) scale(.98); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }
  `,
  host: {
    '(document:keydown.escape)': 'closed.emit()',
  },
})
export class Dialog {
  readonly title = input.required<string>();
  readonly closed = output<void>();
}
