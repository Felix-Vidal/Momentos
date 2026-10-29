import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  resource,
  signal,
} from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Spinner } from '../../shared/ui/spinner';
import { DefaultHero } from './themes/default-hero';
import { RouletteHero } from './themes/roulette-hero';
import { GroupsService } from './groups.service';
import { UploadDialog } from '../media/upload.dialog';
import { MediaService } from '../media/media.service';

/**
 * Moldura de um grupo: hero conforme o tema e as abas da linha do tempo,
 * integrantes e configurações. O grupo carregado é compartilhado com as abas
 * pelo `GroupContext`.
 */
@Component({
  selector: 'app-group-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    Spinner,
    DefaultHero,
    RouletteHero,
    UploadDialog,
  ],
  template: `
    @if (group.isLoading()) {
      <app-spinner label="Abrindo o grupo…" />
    } @else if (group.error()) {
      <section class="flex flex-col items-center gap-3 py-16 text-center">
        <span class="text-4xl" aria-hidden="true">🔒</span>
        <h1 class="text-xl font-semibold">Este grupo não está disponível para você</h1>
        <p class="max-w-sm text-sm text-muted">
          O grupo pode ter sido removido, ou o conteúdo é visível apenas para quem participa.
          Grupos abertos aceitam pedidos de entrada na página Descobrir.
        </p>
        <a
          routerLink="/descobrir"
          class="rounded-full border border-line px-5 py-2.5 text-sm text-muted transition
                 hover:border-brand hover:text-ink"
        >Ver grupos abertos</a>
      </section>
    } @else if (group.value(); as g) {
      <section class="flex flex-col gap-6">
        @if (g.theme === 'ROULETTE') {
          <app-roulette-hero [name]="g.name" [description]="g.description" />
        } @else {
          <app-default-hero [name]="g.name" [description]="g.description" />
        }

        <nav class="flex flex-wrap items-center gap-1 border-b border-line/60 pb-px text-sm">
          <a
            [routerLink]="['/g', g.slug]"
            routerLinkActive="border-brand text-ink"
            [routerLinkActiveOptions]="{ exact: true }"
            class="rounded-t-lg border-b-2 border-transparent px-4 py-2 text-muted transition hover:text-ink"
          >Linha do tempo</a>
          <a
            [routerLink]="['/g', g.slug, 'integrantes']"
            routerLinkActive="border-brand text-ink"
            class="rounded-t-lg border-b-2 border-transparent px-4 py-2 text-muted transition hover:text-ink"
          >Integrantes <span class="text-faint">{{ g.memberCount }}</span></a>
          @if (g.myRole === 'ADMIN') {
            <a
              [routerLink]="['/g', g.slug, 'configuracoes']"
              routerLinkActive="border-brand text-ink"
              class="rounded-t-lg border-b-2 border-transparent px-4 py-2 text-muted transition hover:text-ink"
            >Configurações</a>
          }

          <button
            type="button"
            class="ml-auto mb-1 rounded-full bg-brand px-4 py-1.5 text-sm font-medium text-base
                   transition hover:bg-brand-soft active:scale-95"
            (click)="uploading.set(true)"
          >Enviar mídias</button>
        </nav>

        <router-outlet />
      </section>

      @if (uploading()) {
        <app-upload-dialog
          [groupId]="g.id"
          (closed)="uploading.set(false)"
          (uploaded)="afterUpload()"
        />
      }
    }
  `,
})
export class GroupShellPage {
  /** Vem da rota /g/:slug por componentInputBinding. */
  readonly slug = input.required<string>();

  private readonly service = inject(GroupsService);
  private readonly media = inject(MediaService);

  protected readonly uploading = signal(false);

  readonly group = resource({
    params: () => ({ slug: this.slug(), revision: this.service.revision() }),
    loader: ({ params }) => this.service.bySlug(params.slug),
  });

  readonly groupId = computed(() => this.group.value()?.id ?? null);

  protected afterUpload(): void {
    this.uploading.set(false);
    // A linha do tempo observa a revisão de mídias e recarrega sozinha.
    this.media.revision.update((n) => n + 1);
  }
}
