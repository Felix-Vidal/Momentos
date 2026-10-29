import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast-host',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pointer-events-none fixed inset-x-0 bottom-4 z-100 flex flex-col items-center gap-2 px-4">
      @for (toast of toasts(); track toast.id) {
        <div
          class="glass pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-2xl px-4 py-3 text-sm
                 animate-[rise_.35s_var(--ease-soft)]"
          role="status"
        >
          <span
            class="mt-0.5 shrink-0"
            [class.text-danger]="toast.kind === 'error'"
            [class.text-success]="toast.kind === 'success'"
            [class.text-brand-soft]="toast.kind === 'info'"
          >
            @switch (toast.kind) {
              @case ('error') { ⚠ }
              @case ('success') { ✓ }
              @default { ℹ }
            }
          </span>
          <p class="flex-1 text-ink">{{ toast.text }}</p>
          <button
            type="button"
            class="shrink-0 text-faint transition hover:text-ink"
            (click)="service.dismiss(toast.id)"
            aria-label="Fechar aviso"
          >×</button>
        </div>
      }
    </div>
  `,
  styles: `
    @keyframes rise {
      from { opacity: 0; transform: translateY(12px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `,
})
export class ToastHost {
  protected readonly service = inject(ToastService);
  protected readonly toasts = this.service.toasts;
}
