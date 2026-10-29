import { ChangeDetectionStrategy, Component, computed, inject, resource } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { injectGroup } from '../groups/group-context';
import { GroupsService } from '../groups/groups.service';
import { Avatar } from '../../shared/ui/avatar';
import { Spinner } from '../../shared/ui/spinner';
import { ToastService } from '../../shared/ui/toast.service';

@Component({
  selector: 'app-members-tab',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Avatar, Spinner],
  template: `
    @if (members.isLoading()) {
      <app-spinner label="Carregando integrantes…" />
    } @else {
      <ul class="grid gap-3 sm:grid-cols-2">
        @for (member of members.value(); track member.userId) {
          <li
            class="flex items-center gap-3 rounded-box border border-line bg-surface/40 p-4
                   transition hover:border-brand/50"
          >
            <app-avatar [name]="member.name" [mediaId]="member.avatarMediaId" />
            <div class="flex min-w-0 flex-1 flex-col">
              <a
                [routerLink]="['/g', group.value()?.slug, 'integrantes', member.userId]"
                class="truncate text-sm font-medium transition hover:text-brand-soft"
              >{{ member.name }}</a>
              <span class="truncate text-xs text-faint">
                {{ member.role === 'ADMIN' ? 'Administrador' : 'Integrante' }}
                @if (member.bio) { · {{ member.bio }} }
              </span>
            </div>

            @if (amAdmin() && member.userId !== myId()) {
              <div class="flex shrink-0 items-center gap-1">
                <button
                  type="button"
                  class="rounded-full border border-line px-2.5 py-1 text-xs text-muted transition
                         hover:border-brand hover:text-ink"
                  (click)="toggleRole(member.userId, member.role)"
                >{{ member.role === 'ADMIN' ? 'Rebaixar' : 'Promover' }}</button>
                <button
                  type="button"
                  class="rounded-full border border-line px-2.5 py-1 text-xs text-muted transition
                         hover:border-danger hover:text-danger"
                  (click)="remove(member.userId, member.name)"
                >Remover</button>
              </div>
            }
          </li>
        }
      </ul>
    }
  `,
})
export class MembersTab {
  private readonly service = inject(GroupsService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly group = injectGroup().group;
  private readonly groupId = injectGroup().groupId;

  protected readonly myId = computed(() => this.auth.currentUser()?.id ?? null);
  protected readonly amAdmin = computed(() => this.group.value()?.myRole === 'ADMIN');

  protected readonly members = resource({
    params: () => ({ id: this.groupId(), revision: this.service.revision() }),
    loader: ({ params }) => (params.id ? this.service.members(params.id) : Promise.resolve([])),
  });

  protected async toggleRole(userId: string, role: 'ADMIN' | 'MEMBER'): Promise<void> {
    const id = this.groupId();
    if (!id) {
      return;
    }
    await this.service.changeRole(id, userId, role === 'ADMIN' ? 'MEMBER' : 'ADMIN');
    this.toast.success(role === 'ADMIN' ? 'Agora é integrante.' : 'Agora é administrador.');
    this.members.reload();
  }

  protected async remove(userId: string, name: string): Promise<void> {
    const id = this.groupId();
    if (!id || !confirm(`Remover ${name} do grupo?`)) {
      return;
    }
    await this.service.removeMember(id, userId);
    this.toast.success(`${name} saiu do grupo.`);
    this.members.reload();
  }
}
