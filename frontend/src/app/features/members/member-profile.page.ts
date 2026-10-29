import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, input, resource } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { mediaThumbnailUrl } from '../../core/api/media.models';
import { UserProfile } from '../../core/auth/auth.models';
import { injectGroup } from '../groups/group-context';
import { MediaService } from '../media/media.service';
import { Avatar } from '../../shared/ui/avatar';
import { EmptyState } from '../../shared/ui/empty-state';
import { Spinner } from '../../shared/ui/spinner';

/** Perfil de um integrante dentro do grupo: quem é e o que publicou aqui. */
@Component({
  selector: 'app-member-profile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar, EmptyState, Spinner],
  template: `
    @if (profile.isLoading()) {
      <app-spinner />
    } @else if (profile.value(); as user) {
      <section class="flex flex-col gap-6">
        <header class="flex items-center gap-4">
          <app-avatar [name]="user.name" [mediaId]="user.avatarMediaId" size="lg" />
          <div class="flex flex-col gap-1">
            <h2 class="text-xl font-semibold">{{ user.name }}</h2>
            @if (user.bio) {
              <p class="max-w-prose text-sm text-muted">{{ user.bio }}</p>
            }
            <p class="text-xs text-faint">
              {{ media.value()?.length ?? 0 }} mídia(s) publicada(s) neste grupo
            </p>
          </div>
        </header>

        @if (media.value()?.length) {
          <ul class="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
            @for (item of media.value(); track item.id) {
              <li class="group relative aspect-square overflow-hidden rounded-xl bg-surface">
                <img
                  [src]="thumbnail(item)"
                  [alt]="item.caption ?? 'Mídia de ' + user.name"
                  loading="lazy"
                  class="size-full object-cover transition duration-500 ease-[var(--ease-soft)]
                         group-hover:scale-105"
                />
                @if (item.type === 'VIDEO') {
                  <span
                    class="absolute right-2 top-2 rounded-full bg-black/60 px-2 py-0.5 text-xs"
                    aria-hidden="true"
                  >▶</span>
                }
              </li>
            }
          </ul>
        } @else {
          <app-empty-state
            icon="◌"
            title="Nada publicado por aqui ainda"
            hint="As fotos e vídeos que esta pessoa compartilhar no grupo aparecem aqui."
          />
        }
      </section>
    }
  `,
})
export class MemberProfilePage {
  readonly userId = input.required<string>();

  private readonly http = inject(HttpClient);
  private readonly mediaService = inject(MediaService);
  private readonly context = injectGroup();

  protected readonly thumbnail = mediaThumbnailUrl;

  protected readonly profile = resource({
    params: () => this.userId(),
    loader: ({ params }) => firstValueFrom(this.http.get<UserProfile>(`/api/users/${params}`)),
  });

  protected readonly media = resource({
    params: () => ({
      userId: this.userId(),
      groupId: this.context.groupId(),
      revision: this.mediaService.revision(),
    }),
    loader: ({ params }) =>
      params.groupId
        ? this.mediaService.byMemberInGroup(params.userId, params.groupId)
        : Promise.resolve([]),
  });
}
