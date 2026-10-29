import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, input, resource } from '@angular/core';
import { UserProfile } from '../../core/auth/auth.models';
import { Avatar } from '../../shared/ui/avatar';
import { Spinner } from '../../shared/ui/spinner';
import { firstValueFrom } from 'rxjs';

/** Perfil de um integrante dentro do grupo: quem é e o que publicou. */
@Component({
  selector: 'app-member-profile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar, Spinner],
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
          </div>
        </header>

        <p class="text-sm text-faint">
          As mídias publicadas por {{ user.name }} aparecem aqui quando o envio de fotos
          estiver disponível.
        </p>
      </section>
    }
  `,
})
export class MemberProfilePage {
  readonly userId = input.required<string>();

  private readonly http = inject(HttpClient);

  protected readonly profile = resource({
    params: () => this.userId(),
    loader: ({ params }) =>
      firstValueFrom(this.http.get<UserProfile>(`/api/users/${params}`)),
  });
}
