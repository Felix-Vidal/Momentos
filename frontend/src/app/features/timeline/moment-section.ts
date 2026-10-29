import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { MediaView } from '../../core/api/media.models';
import { MomentBlock } from '../../core/api/timeline.models';
import { AuthService } from '../../core/auth/auth.service';
import { formatInterval } from '../../core/util/date.util';
import { CommentCarousel } from '../comments/comment-carousel';
import { HoverOrTapDirective } from '../../shared/directives/hover-or-tap.directive';
import { InViewDirective } from '../../shared/directives/in-view.directive';

/**
 * Bloco de um Momento na linha do tempo: o título, o intervalo e as mídias que
 * caíram nele.
 *
 * <p>No hover do título (ou no primeiro toque, no celular) a caixa de comentários
 * aparece abaixo dele e percorre o conjunto inteiro — o que foi dito no Momento e
 * em cada uma das suas mídias, cada comentário com a miniatura da foto a que se
 * refere.
 */
@Component({
  selector: 'app-moment-section',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InViewDirective, HoverOrTapDirective, CommentCarousel],
  template: `
    <section
      appInView
      class="relative flex flex-col gap-4 transition duration-500 ease-[var(--ease-soft)]
             data-[in-view=false]:translate-y-8 data-[in-view=false]:opacity-0
             data-[in-view=true]:translate-y-0 data-[in-view=true]:opacity-100"
    >
      <header class="relative flex flex-wrap items-end justify-between gap-2">
        <div class="relative flex min-w-0 flex-col gap-1">
          <!-- O título é o gatilho da caixa de comentários do conjunto. -->
          <h2 class="relative w-fit font-display text-xl">
            <span
              appHoverOrTap
              #titleTrigger="hoverOrTap"
              tabindex="0"
              class="relative inline-flex cursor-pointer items-center gap-2 rounded-lg outline-none
                     transition hover:text-brand-soft focus-visible:ring-2 focus-visible:ring-brand"
              [attr.aria-label]="
                'Ver comentários de ' + block().moment.title + ' (' + block().commentCount + ')'
              "
              (confirmed)="openComments.emit(block())"
            >
              <span
                class="h-5 w-1 rounded-full bg-gradient-to-b from-brand to-accent"
                aria-hidden="true"
              ></span>
              <ng-content select="[slot=title]" />
              @if (block().commentCount > 0) {
                <span class="text-xs text-faint">{{ block().commentCount }} 💬</span>
              }
            </span>

            <app-comment-carousel
              placement="below"
              [comments]="block().commentFeed"
              [visible]="titleTrigger.isActive()"
              [showMediaThumbnail]="true"
              emptyMessage="Nenhum comentário neste momento ainda"
            />
          </h2>
          <p class="text-xs text-faint">
            {{ interval() }}
            <span aria-hidden="true">·</span>
            {{ block().moment.mediaCount }}
            {{ block().moment.mediaCount === 1 ? 'mídia' : 'mídias' }}
            @if (block().commentCount > 0) {
              <span aria-hidden="true">·</span>
              {{ block().commentCount }}
              {{ block().commentCount === 1 ? 'comentário' : 'comentários' }}
            }
          </p>
          @if (block().moment.description) {
            <p class="max-w-prose text-sm text-muted">{{ block().moment.description }}</p>
          }
        </div>

        @if (canEdit()) {
          <div class="flex shrink-0 items-center gap-1">
            <button
              type="button"
              class="rounded-full border border-line px-3 py-1 text-xs text-muted transition
                     hover:border-brand hover:text-ink"
              (click)="edit.emit(block())"
            >Editar</button>
            <button
              type="button"
              class="rounded-full border border-line px-3 py-1 text-xs text-muted transition
                     hover:border-danger hover:text-danger"
              (click)="remove.emit(block())"
            >Apagar</button>
          </div>
        }
      </header>

      @if (block().media.length) {
        <ng-content />
      } @else {
        <p class="rounded-box border border-dashed border-line px-4 py-6 text-center text-sm text-faint">
          Nenhuma mídia nesse intervalo ainda. Envie fotos desse período e elas aparecem aqui.
        </p>
      }
    </section>
  `,
})
export class MomentSection {
  readonly block = input.required<MomentBlock>();
  readonly isAdmin = input(false);
  readonly edit = output<MomentBlock>();
  readonly remove = output<MomentBlock>();
  readonly openMedia = output<MediaView>();
  /** Clique (ou segundo toque) no título: abrir a conversa completa do Momento. */
  readonly openComments = output<MomentBlock>();

  private readonly auth = inject(AuthService);

  protected readonly interval = computed(() =>
    formatInterval(this.block().moment.startsAt, this.block().moment.endsAt),
  );

  /** Quem criou o Momento pode editá-lo; administradores do grupo também. */
  protected readonly canEdit = computed(
    () => this.isAdmin() || this.block().moment.createdBy.id === this.auth.currentUser()?.id,
  );
}
