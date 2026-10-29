import { ChangeDetectionStrategy, Component, inject, input, output, resource, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { formatDayAndTime } from '../../core/util/date.util';
import { Dialog } from '../../shared/ui/dialog';
import { ToastService } from '../../shared/ui/toast.service';
import { InvitesService, InviteView } from './invites.service';

/**
 * Convites do grupo: gera um link compartilhável ou um convite dirigido a um e-mail,
 * e lista o que já foi gerado para revogar quando necessário.
 */
@Component({
  selector: 'app-invite-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Dialog],
  template: `
    <app-dialog title="Convidar para o grupo" (closed)="closed.emit()">
      <div class="flex flex-col gap-5">
        <div class="flex flex-col gap-3">
          <label class="flex flex-col gap-1.5">
            <span class="text-sm text-muted">E-mail de quem você quer convidar (opcional)</span>
            <input
              type="email"
              [(ngModel)]="email"
              name="email"
              placeholder="deixe vazio para gerar um link aberto"
              class="rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                     transition focus:border-brand focus:ring-2 focus:ring-brand/30"
            />
          </label>

          <div class="grid gap-3 sm:grid-cols-3">
            <label class="flex flex-col gap-1.5">
              <span class="text-sm text-muted">Entra como</span>
              <select
                [(ngModel)]="role"
                name="role"
                class="rounded-xl border border-line bg-surface/60 px-3 py-2.5 text-sm outline-none
                       focus:border-brand"
              >
                <option value="MEMBER">Integrante</option>
                <option value="ADMIN">Administrador</option>
              </select>
            </label>
            <label class="flex flex-col gap-1.5">
              <span class="text-sm text-muted">Validade (horas)</span>
              <input
                type="number"
                min="1"
                max="8760"
                [(ngModel)]="hours"
                name="hours"
                class="rounded-xl border border-line bg-surface/60 px-3 py-2.5 text-sm outline-none
                       focus:border-brand"
              />
            </label>
            <label class="flex flex-col gap-1.5">
              <span class="text-sm text-muted">Usos</span>
              <input
                type="number"
                min="1"
                max="100"
                [(ngModel)]="maxUses"
                name="maxUses"
                [disabled]="!!email.trim()"
                class="rounded-xl border border-line bg-surface/60 px-3 py-2.5 text-sm outline-none
                       focus:border-brand disabled:opacity-50"
              />
            </label>
          </div>

          @if (lastLink()) {
            <div class="flex flex-col gap-2 rounded-xl border border-brand/50 bg-brand/10 p-3">
              <p class="text-xs text-muted">
                Link gerado — ele aparece só agora, então copie antes de fechar:
              </p>
              <div class="flex items-center gap-2">
                <code class="min-w-0 flex-1 truncate rounded-lg bg-base/60 px-2.5 py-2 text-xs">
                  {{ lastLink() }}
                </code>
                <button
                  type="button"
                  class="shrink-0 rounded-full bg-brand px-3 py-1.5 text-xs font-medium text-base
                         transition hover:bg-brand-soft"
                  (click)="copy()"
                >Copiar</button>
              </div>
            </div>
          }
        </div>

        <section class="flex flex-col gap-2 border-t border-line/60 pt-4">
          <h3 class="text-sm text-muted">Convites já gerados</h3>
          @if (invites.value()?.length) {
            <ul class="flex flex-col gap-2">
              @for (invite of invites.value(); track invite.id) {
                <li class="flex items-center gap-3 rounded-xl border border-line bg-surface/40 p-2.5">
                  <div class="flex min-w-0 flex-1 flex-col">
                    <span class="truncate text-sm">
                      {{ invite.invitedEmail ?? 'Link aberto' }}
                      <span class="text-xs text-faint">
                        · {{ invite.role === 'ADMIN' ? 'administrador' : 'integrante' }}
                      </span>
                    </span>
                    <span class="text-xs text-faint">
                      {{ invite.uses }}/{{ invite.maxUses }} usos · vale até
                      {{ when(invite.expiresAt) }}
                      @if (invite.revoked) { · revogado }
                    </span>
                  </div>
                  @if (!invite.revoked) {
                    <button
                      type="button"
                      class="shrink-0 rounded-full border border-line px-3 py-1 text-xs text-muted
                             transition hover:border-danger hover:text-danger"
                      (click)="revoke(invite)"
                    >Revogar</button>
                  }
                </li>
              }
            </ul>
          } @else {
            <p class="text-sm text-faint">Nenhum convite gerado ainda.</p>
          }
        </section>
      </div>

      <button
        slot="actions"
        type="button"
        class="rounded-full border border-line px-4 py-2 text-sm text-muted transition hover:text-ink"
        (click)="closed.emit()"
      >Fechar</button>

      <button
        slot="actions"
        type="button"
        [disabled]="creating()"
        class="rounded-full bg-brand px-5 py-2 text-sm font-medium text-base transition
               hover:bg-brand-soft active:scale-95 disabled:opacity-60"
        (click)="create()"
      >{{ creating() ? 'Gerando…' : 'Gerar convite' }}</button>
    </app-dialog>
  `,
})
export class InviteDialog {
  readonly groupId = input.required<string>();
  readonly closed = output<void>();

  private readonly service = inject(InvitesService);
  private readonly toast = inject(ToastService);

  protected email = '';
  protected role: 'MEMBER' | 'ADMIN' = 'MEMBER';
  protected hours = 168;
  protected maxUses = 1;

  protected readonly creating = signal(false);
  protected readonly lastLink = signal<string | null>(null);
  protected readonly when = formatDayAndTime;

  protected readonly invites = resource({
    params: () => this.groupId(),
    loader: ({ params }) => this.service.list(params),
  });

  protected async create(): Promise<void> {
    this.creating.set(true);
    try {
      const invite = await this.service.create(this.groupId(), {
        email: this.email.trim() || null,
        role: this.role,
        expiresInHours: this.hours,
        maxUses: this.email.trim() ? 1 : this.maxUses,
      });
      if (invite.token) {
        this.lastLink.set(this.service.linkFor(invite.token));
      }
      this.toast.success(
        this.email.trim() ? `Convite gerado para ${this.email.trim()}.` : 'Link de convite gerado.',
      );
      this.email = '';
      this.invites.reload();
    } finally {
      this.creating.set(false);
    }
  }

  protected async copy(): Promise<void> {
    const link = this.lastLink();
    if (!link) {
      return;
    }
    try {
      await navigator.clipboard.writeText(link);
      this.toast.success('Link copiado.');
    } catch {
      // Sem permissão de área de transferência, o link continua visível para copiar à mão.
      this.toast.info('Copie o link manualmente.');
    }
  }

  protected async revoke(invite: InviteView): Promise<void> {
    await this.service.revoke(this.groupId(), invite.id);
    this.toast.success('Convite revogado.');
    this.invites.reload();
  }
}
