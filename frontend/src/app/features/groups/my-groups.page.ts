import { ChangeDetectionStrategy, Component, inject, resource } from '@angular/core';
import { RouterLink } from '@angular/router';
import { GroupsService } from './groups.service';
import { InvitesService } from '../invites/invites.service';
import { EmptyState } from '../../shared/ui/empty-state';
import { Spinner } from '../../shared/ui/spinner';

@Component({
  selector: 'app-my-groups',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, EmptyState, Spinner],
  template: `
    <section class="flex flex-col gap-6">
      <header class="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 class="text-2xl font-semibold">Meus grupos</h1>
          <p class="text-sm text-muted">As linhas do tempo das quais você participa.</p>
        </div>
        <a
          routerLink="/criar-grupo"
          class="rounded-full bg-brand px-4 py-2 text-sm font-medium text-base transition
                 hover:bg-brand-soft active:scale-95"
        >Criar grupo</a>
      </header>

      @if (invites.value()?.length) {
        <section class="flex flex-col gap-2 rounded-box border border-brand/40 bg-brand/10 p-4">
          <h2 class="text-sm font-medium">Você tem convite para entrar</h2>
          <ul class="flex flex-col gap-2">
            @for (invite of invites.value(); track invite.id) {
              <li class="flex items-center justify-between gap-3 rounded-xl bg-surface/50 p-3">
                <span class="text-sm text-muted">
                  Convite como
                  {{ invite.role === 'ADMIN' ? 'administrador' : 'integrante' }}
                </span>
                <span class="text-xs text-faint">
                  Use o link que você recebeu para aceitar
                </span>
              </li>
            }
          </ul>
        </section>
      }

      @if (groups.isLoading()) {
        <app-spinner label="Buscando seus grupos…" />
      } @else if (groups.value()?.length) {
        <ul class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          @for (group of groups.value(); track group.id) {
            <li>
              <a
                [routerLink]="['/g', group.slug]"
                class="group flex h-full flex-col gap-3 rounded-box border border-line bg-surface/50 p-5
                       transition duration-300 ease-[var(--ease-soft)] hover:-translate-y-1
                       hover:border-brand/60 hover:shadow-[var(--shadow-glow)]"
              >
                <div class="flex items-center justify-between gap-2">
                  <h2 class="font-display text-lg">{{ group.name }}</h2>
                  @if (group.theme === 'ROULETTE') {
                    <span
                      class="text-lg transition-transform duration-700 ease-[var(--ease-soft)] group-hover:rotate-180"
                      aria-hidden="true"
                    >◉</span>
                  }
                </div>
                @if (group.description) {
                  <p class="line-clamp-3 flex-1 text-sm text-muted">{{ group.description }}</p>
                }
                <footer class="flex items-center gap-2 text-xs text-faint">
                  <span>{{ group.memberCount }} {{ group.memberCount === 1 ? 'integrante' : 'integrantes' }}</span>
                  <span aria-hidden="true">·</span>
                  <span>{{ group.visibility === 'OPEN' ? 'Aberto' : 'Privado' }}</span>
                  @if (group.myRole === 'ADMIN') {
                    <span aria-hidden="true">·</span>
                    <span class="text-brand-soft">admin</span>
                  }
                </footer>
              </a>
            </li>
          }
        </ul>
      } @else {
        <app-empty-state
          icon="◍"
          title="Você ainda não está em nenhum grupo"
          hint="Crie o grupo dos seus amigos ou procure um grupo aberto para pedir entrada."
        >
          <div class="mt-3 flex flex-wrap justify-center gap-2">
            <a
              routerLink="/criar-grupo"
              class="rounded-full bg-brand px-4 py-2 text-sm font-medium text-base transition hover:bg-brand-soft"
            >Criar grupo</a>
            <a
              routerLink="/descobrir"
              class="rounded-full border border-line px-4 py-2 text-sm text-muted transition
                     hover:border-brand hover:text-ink"
            >Descobrir grupos</a>
          </div>
        </app-empty-state>
      }
    </section>
  `,
})
export class MyGroupsPage {
  private readonly service = inject(GroupsService);
  private readonly invitesService = inject(InvitesService);

  // Recarrega sozinho quando algo muda em grupos (criar, sair, trocar papel).
  protected readonly groups = resource({
    params: () => this.service.revision(),
    loader: () => this.service.mine(),
  });

  protected readonly invites = resource({
    params: () => this.service.revision(),
    loader: () => this.invitesService.mine(),
  });
}
