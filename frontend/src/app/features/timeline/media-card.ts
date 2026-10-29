import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MediaView, mediaThumbnailUrl } from '../../core/api/media.models';
import { TimelineMedia } from '../../core/api/timeline.models';
import { formatDayAndTime, formatDuration } from '../../core/util/date.util';
import { CommentCarousel } from '../comments/comment-carousel';
import { HoverOrTapDirective } from '../../shared/directives/hover-or-tap.directive';
import { InViewDirective } from '../../shared/directives/in-view.directive';

/**
 * Cartão de uma foto ou vídeo na linha do tempo.
 *
 * <p>No hover (ou no primeiro toque, no celular) a caixa de comentários sobe na base
 * da mídia e percorre a conversa; o clique — ou o segundo toque — abre o lightbox.
 */
@Component({
  selector: 'app-media-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InViewDirective, HoverOrTapDirective, CommentCarousel],
  template: `
    <figure
      appInView
      appHoverOrTap
      #trigger="hoverOrTap"
      tabindex="0"
      class="group relative cursor-zoom-in overflow-hidden rounded-box bg-surface outline-none
             transition duration-500 ease-[var(--ease-soft)]
             data-[in-view=false]:translate-y-6 data-[in-view=false]:opacity-0
             data-[in-view=true]:translate-y-0 data-[in-view=true]:opacity-100
             hover:shadow-[var(--shadow-glow)] focus-visible:ring-2 focus-visible:ring-brand"
      [attr.aria-label]="'Abrir ' + label() + ' de ' + media().owner.name"
      (confirmed)="opened.emit(media())"
    >
      <img
        [src]="thumbnail()"
        [alt]="media().caption ?? label() + ' de ' + media().owner.name"
        loading="lazy"
        [style.aspect-ratio]="aspectRatio()"
        class="w-full object-cover transition duration-700 ease-[var(--ease-soft)]
               group-hover:scale-[1.04]"
      />

      @if (media().type === 'VIDEO') {
        <span
          class="pointer-events-none absolute left-3 top-3 flex items-center gap-1 rounded-full
                 bg-black/60 px-2 py-0.5 text-xs backdrop-blur-sm"
        >
          <span aria-hidden="true">▶</span>
          @if (duration()) { {{ duration() }} }
        </span>
      }

      @if (item().commentCount > 0) {
        <span
          class="pointer-events-none absolute right-3 top-3 rounded-full bg-black/60 px-2 py-0.5
                 text-xs backdrop-blur-sm"
        >
          {{ item().commentCount }} <span aria-hidden="true">💬</span>
        </span>
      }

      <figcaption
        class="pointer-events-none absolute inset-x-0 top-0 flex items-start justify-between gap-2
               bg-gradient-to-b from-black/60 to-transparent p-3 text-xs opacity-0
               transition duration-300 group-hover:opacity-100"
      >
        <span class="truncate">{{ media().owner.name }}</span>
        <span class="shrink-0 text-muted">{{ when() }}</span>
      </figcaption>

      <app-comment-carousel [comments]="item().comments" [visible]="trigger.isActive()" />
    </figure>
  `,
})
export class MediaCard {
  readonly item = input.required<TimelineMedia>();
  readonly opened = output<MediaView>();

  protected readonly media = computed(() => this.item().media);
  protected readonly thumbnail = computed(() => mediaThumbnailUrl(this.media()));
  protected readonly when = computed(() => formatDayAndTime(this.media().capturedAt));
  protected readonly duration = computed(() => formatDuration(this.media().durationSeconds));
  protected readonly label = computed(() => (this.media().type === 'VIDEO' ? 'vídeo' : 'foto'));

  /** Mantém a proporção original quando conhecida, para o grid não "pular". */
  protected readonly aspectRatio = computed(() => {
    const { width, height } = this.media();
    return width && height ? `${width} / ${height}` : '1 / 1';
  });
}
