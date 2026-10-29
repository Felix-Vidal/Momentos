import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast-host',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pointer-events-none fixed inset-x-0 bottom-4 z-100 flex flex-col items-center gap-2 px-4">
      @for (toast of toasts(); track toast.id) {
        <div
          class="vidro pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-2xl px-4 py-3 text-sm
                 animate-[subir_.35s_var(--ease-suave)]"
          [class.text-perigo]="toast.tipo === 'erro'"
          [class.text-sucesso]="toast.tipo === 'sucesso'"
          role="status"
        >
          <span class="mt-0.5 shrink-0">
            @switch (toast.tipo) {
              @case ('erro') { ⚠ }
              @case ('sucesso') { ✓ }
              @default { ℹ }
            }
          </span>
          <p class="flex-1 text-texto">{{ toast.texto }}</p>
          <button
            type="button"
            class="shrink-0 text-texto-fraco transition hover:text-texto"
            (click)="service.fechar(toast.id)"
            aria-label="Fechar aviso"
          >×</button>
        </div>
      }
    </div>
  `,
  styles: `
    @keyframes subir {
      from { opacity: 0; transform: translateY(12px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `,
})
export class ToastHost {
  protected readonly service = inject(ToastService);
  protected readonly toasts = this.service.toasts;
}
