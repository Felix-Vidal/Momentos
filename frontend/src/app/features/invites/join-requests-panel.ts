import { ChangeDetectionStrategy, Component, inject, input, resource } from '@angular/core';
import { formatDayAndTime } from '../../core/util/date.util';
import { Avatar } from '../../shared/ui/avatar';
import { ToastService } from '../../shared/ui/toast.service';
import { JoinRequestsService, JoinRequestView } from './join-requests.service';

/** Pedidos de entrada aguardando resposta de um administrador. */
@Component({
  selector: 'app-join-requests-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar],
  template: `
    @if (requests.value()?.length) {
      <section class="flex flex-col gap-3 rounded-box border border-accent/40 bg-accent/5 p-4">
        <header class="flex items-center gap-2">
          <h3 class="text-sm font-medium">Pedidos para entrar</h3>
          <span class="rounded-full bg-accent/20 px-2 py-0.5 text-xs text-accent">
            {{ requests.value()!.length }}
          </span>
        </header>

        <ul class="flex flex-col gap-2">
          @for (request of requests.value(); track request.id) {
            <li class="flex items-start gap-3 rounded-xl bg-surface/50 p-3">
              <app-avatar [name]="request.userName" [mediaId]="request.userAvatarMediaId" />
              <div class="flex min-w-0 flex-1 flex-col gap-0.5">
                <span class="text-sm font-medium">{{ request.userName }}</span>
                <span class="text-xs text-faint">{{ when(request.createdAt) }}</span>
                @if (request.message) {
                  <p class="mt-1 text-sm text-muted">“{{ request.message }}”</p>
                }
              </div>
              <div class="flex shrink-0 flex-col gap-1.5 sm:flex-row">
                <button
                  type="button"
                  class="rounded-full bg-brand px-3 py-1.5 text-xs font-medium text-base
                         transition hover:bg-brand-soft active:scale-95"
                  (click)="approve(request)"
                >Aprovar</button>
                <button
                  type="button"
                  class="rounded-full border border-line px-3 py-1.5 text-xs text-muted
                         transition hover:border-danger hover:text-danger"
                  (click)="reject(request)"
                >Recusar</button>
              </div>
            </li>
          }
        </ul>
      </section>
    }
  `,
})
export class JoinRequestsPanel {
  readonly groupId = input.required<string>();

  private readonly service = inject(JoinRequestsService);
  private readonly toast = inject(ToastService);

  protected readonly when = formatDayAndTime;

  protected readonly requests = resource({
    params: () => this.groupId(),
    loader: ({ params }) => this.service.pending(params),
  });

  protected async approve(request: JoinRequestView): Promise<void> {
    await this.service.approve(this.groupId(), request.id);
    this.toast.success(`${request.userName} entrou no grupo.`);
    this.requests.reload();
  }

  protected async reject(request: JoinRequestView): Promise<void> {
    await this.service.reject(this.groupId(), request.id);
    this.toast.info(`Pedido de ${request.userName} recusado.`);
    this.requests.reload();
  }
}
