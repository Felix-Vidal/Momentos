import { ChangeDetectionStrategy, Component, inject, input, resource, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { Spinner } from '../../shared/ui/spinner';
import { ToastService } from '../../shared/ui/toast.service';
import { InvitesService } from './invites.service';

/** Tela que abre quando alguém acessa um link de convite. */
@Component({
  selector: 'app-join-by-token',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Spinner],
  template: `
    <section class="mx-auto flex w-full max-w-md flex-col items-center gap-5 py-10 text-center">
      @if (preview.isLoading()) {
        <app-spinner label="Conferindo o convite…" />
      } @else if (preview.value(); as invite) {
        @if (invite.valid || invite.alreadyMember) {
          <span class="text-4xl" aria-hidden="true">✉</span>
          <div class="flex flex-col gap-1">
            <h1 class="text-2xl font-semibold">{{ invite.groupName }}</h1>
            @if (invite.invitedBy) {
              <p class="text-sm text-muted">Convite de {{ invite.invitedBy }}</p>
            }
          </div>
          @if (invite.groupDescription) {
            <p class="text-sm text-muted">{{ invite.groupDescription }}</p>
          }

          @if (invite.alreadyMember) {
            <p class="text-sm text-success">Você já participa deste grupo.</p>
            <a
              [routerLink]="['/g', invite.groupId]"
              class="rounded-full bg-brand px-5 py-2.5 text-sm font-medium text-base transition
                     hover:bg-brand-soft"
            >Abrir o grupo</a>
          } @else if (!loggedIn()) {
            <p class="text-sm text-muted">Entre na sua conta para aceitar o convite.</p>
            <div class="flex flex-wrap justify-center gap-2">
              <a
                [routerLink]="['/entrar']"
                [queryParams]="{ retornar: '/convite/' + token() }"
                class="rounded-full bg-brand px-5 py-2.5 text-sm font-medium text-base transition
                       hover:bg-brand-soft"
              >Entrar</a>
              <a
                [routerLink]="['/criar-conta']"
                class="rounded-full border border-line px-5 py-2.5 text-sm text-muted transition
                       hover:border-brand hover:text-ink"
              >Criar conta</a>
            </div>
          } @else {
            <button
              type="button"
              [disabled]="accepting()"
              class="rounded-full bg-brand px-5 py-2.5 text-sm font-medium text-base transition
                     hover:bg-brand-soft active:scale-95 disabled:opacity-60"
              (click)="accept()"
            >{{ accepting() ? 'Entrando…' : 'Aceitar convite e entrar' }}</button>
          }
        } @else {
          <span class="text-4xl" aria-hidden="true">⚠</span>
          <h1 class="text-xl font-semibold">Convite indisponível</h1>
          <p class="text-sm text-muted">{{ invite.reason }}</p>
          <a
            routerLink="/"
            class="rounded-full border border-line px-5 py-2.5 text-sm text-muted transition
                   hover:border-brand hover:text-ink"
          >Voltar ao início</a>
        }
      }
    </section>
  `,
})
export class JoinByTokenPage {
  readonly token = input.required<string>();

  private readonly service = inject(InvitesService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly accepting = signal(false);
  protected readonly loggedIn = this.auth.isLoggedIn;

  protected readonly preview = resource({
    params: () => this.token(),
    loader: ({ params }) => this.service.preview(params),
  });

  protected async accept(): Promise<void> {
    this.accepting.set(true);
    try {
      const { groupId } = await this.service.accept(this.token());
      this.toast.success('Bem-vindo ao grupo!');
      await this.router.navigate(['/g', groupId]);
    } catch {
      // A mensagem do servidor já aparece como aviso; a tela recarrega o estado.
      this.preview.reload();
    } finally {
      this.accepting.set(false);
    }
  }
}
