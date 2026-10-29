import { ChangeDetectionStrategy, Component, inject, resource, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyState } from '../../shared/ui/empty-state';
import { Spinner } from '../../shared/ui/spinner';
import { ToastService } from '../../shared/ui/toast.service';
import { GroupsService } from './groups.service';
import { JoinRequestsService } from '../invites/join-requests.service';

@Component({
  selector: 'app-discover-groups',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, EmptyState, Spinner],
  template: `
    <section class="flex flex-col gap-6">
      <header class="flex flex-col gap-1">
        <h1 class="text-2xl font-semibold">Descobrir grupos</h1>
        <p class="text-sm text-muted">
          Grupos abertos aceitam solicitações de entrada. O conteúdo só aparece depois que
          um administrador aprova.
        </p>
      </header>

      <label class="relative">
        <span class="sr-only">Buscar grupos por nome</span>
        <input
          type="search"
          [ngModel]="query()"
          (ngModelChange)="query.set($event)"
          name="busca"
          placeholder="Buscar por nome…"
          class="w-full rounded-full border border-line bg-surface/60 px-4 py-2.5 text-sm outline-none
                 transition focus:border-brand focus:ring-2 focus:ring-brand/30"
        />
      </label>

      @if (groups.isLoading()) {
        <app-spinner label="Procurando grupos…" />
      } @else if (groups.value()?.content?.length) {
        <ul class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          @for (group of groups.value()!.content; track group.id) {
            <li
              class="flex h-full flex-col gap-3 rounded-box border border-line bg-surface/50 p-5
                     transition duration-300 ease-[var(--ease-soft)] hover:-translate-y-1 hover:border-brand/60"
            >
              <h2 class="font-display text-lg">{{ group.name }}</h2>
              @if (group.description) {
                <p class="line-clamp-3 flex-1 text-sm text-muted">{{ group.description }}</p>
              }
              <footer class="flex items-center justify-between gap-2 text-xs text-faint">
                <span>{{ group.memberCount }} {{ group.memberCount === 1 ? 'integrante' : 'integrantes' }}</span>
                @if (group.member) {
                  <button
                    type="button"
                    class="rounded-full border border-line px-3 py-1.5 text-xs text-muted transition
                           hover:border-brand hover:text-ink"
                    (click)="open(group.slug)"
                  >Abrir</button>
                } @else if (group.requested) {
                  <span class="rounded-full bg-base-soft px-3 py-1.5 text-xs text-accent">
                    Pedido enviado
                  </span>
                } @else {
                  <button
                    type="button"
                    [disabled]="asking() === group.id"
                    class="rounded-full bg-brand px-3 py-1.5 text-xs font-medium text-base transition
                           hover:bg-brand-soft active:scale-95 disabled:opacity-60"
                    (click)="ask(group.id)"
                  >Pedir para entrar</button>
                }
              </footer>
            </li>
          }
        </ul>
      } @else {
        <app-empty-state
          icon="◌"
          title="Nenhum grupo aberto encontrado"
          [hint]="query() ? 'Tente outro termo de busca.' : 'Quando alguém criar um grupo aberto, ele aparece aqui.'"
        />
      }
    </section>
  `,
})
export class DiscoverGroupsPage {
  private readonly service = inject(GroupsService);
  private readonly joinRequests = inject(JoinRequestsService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);

  protected readonly query = signal('');
  protected readonly asking = signal<string | null>(null);

  protected readonly groups = resource({
    params: () => ({ q: this.query(), revision: this.service.revision() }),
    loader: ({ params }) => this.service.discover(params.q),
  });

  protected open(slug: string): void {
    void this.router.navigate(['/g', slug]);
  }

  protected async ask(groupId: string): Promise<void> {
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/entrar'], { queryParams: { retornar: '/descobrir' } });
      return;
    }
    this.asking.set(groupId);
    try {
      await this.joinRequests.ask(groupId);
      this.toast.success('Pedido enviado. Um administrador vai avaliar.');
      this.groups.reload();
    } finally {
      this.asking.set(null);
    }
  }
}
