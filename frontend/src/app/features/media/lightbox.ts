import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  resource,
  signal,
} from '@angular/core';
import { MediaView, mediaFileUrl, mediaThumbnailUrl } from '../../core/api/media.models';
import { CommentView } from '../../core/api/timeline.models';
import { formatDayAndTime, formatDuration } from '../../core/util/date.util';
import { CommentForm } from '../comments/comment-form';
import { CommentList } from '../comments/comment-list';
import { CommentsService } from '../comments/comments.service';
import { Spinner } from '../../shared/ui/spinner';
import { ToastService } from '../../shared/ui/toast.service';
import { MediaService } from './media.service';

/**
 * Visualização em tela cheia da mídia, com a conversa completa ao lado (ou abaixo,
 * no celular). É aqui que o texto integral dos comentários aparece — na linha do
 * tempo eles são truncados.
 */
@Component({
  selector: 'app-lightbox',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommentList, CommentForm, Spinner],
  template: `
    <div
      class="fixed inset-0 z-100 flex flex-col bg-black/90 backdrop-blur-sm
             animate-[fade_.2s_ease-out] lg:flex-row"
      role="dialog"
      aria-modal="true"
      aria-label="Mídia em tela cheia"
      (click)="closed.emit()"
    >
      <!-- Mídia -->
      <div class="flex min-h-0 flex-1 items-center justify-center p-3" (click)="$event.stopPropagation()">
        @if (media().type === 'VIDEO') {
          <video
            [src]="fileUrl()"
            [poster]="media().hasThumbnail ? thumbnailUrl() : null"
            controls
            autoplay
            playsinline
            class="max-h-full max-w-full rounded-box"
          ></video>
        } @else {
          <img
            [src]="fileUrl()"
            [alt]="media().caption ?? 'Foto de ' + media().owner.name"
            class="max-h-full max-w-full rounded-box object-contain animate-[zoom_.3s_var(--ease-soft)]"
          />
        }
      </div>

      <!-- Conversa -->
      <aside
        class="glass flex max-h-[55dvh] w-full flex-col gap-4 overflow-hidden rounded-t-box p-4
               lg:max-h-none lg:w-96 lg:rounded-none lg:rounded-l-box"
        (click)="$event.stopPropagation()"
      >
        <header class="flex items-start justify-between gap-3">
          <div class="flex min-w-0 flex-col">
            <span class="truncate text-sm font-medium">{{ media().owner.name }}</span>
            <span class="text-xs text-faint">
              {{ when() }}
              @if (media().capturedAtSource === 'UPLOAD') {
                · data do envio
              }
              @if (duration()) { · {{ duration() }} }
            </span>
          </div>
          <button
            type="button"
            class="grid size-8 shrink-0 place-items-center rounded-full text-muted transition
                   hover:bg-surface hover:text-ink"
            (click)="closed.emit()"
            aria-label="Fechar"
          >×</button>
        </header>

        @if (media().caption) {
          <p class="text-sm text-muted">{{ media().caption }}</p>
        }

        <div class="min-h-0 flex-1 overflow-y-auto pr-1">
          @if (comments.isLoading()) {
            <app-spinner label="Carregando comentários…" />
          } @else {
            <app-comment-list
              [comments]="comments.value() ?? []"
              (edited)="edit($event.comment, $event.body)"
              (removed)="remove($event)"
            />
          }
        </div>

        <app-comment-form [sending]="sending()" (submitted)="send($event)" />
      </aside>
    </div>
  `,
  styles: `
    @keyframes fade { from { opacity: 0; } to { opacity: 1; } }
    @keyframes zoom {
      from { opacity: 0; transform: scale(.96); }
      to { opacity: 1; transform: scale(1); }
    }
  `,
  host: {
    '(document:keydown.escape)': 'closed.emit()',
  },
})
export class Lightbox {
  readonly media = input.required<MediaView>();
  readonly groupId = input.required<string>();
  readonly closed = output<void>();
  readonly changed = output<void>();

  private readonly service = inject(CommentsService);
  private readonly mediaService = inject(MediaService);
  private readonly toast = inject(ToastService);

  protected readonly sending = signal(false);
  protected readonly fileUrl = computed(() => mediaFileUrl(this.media().id));
  protected readonly thumbnailUrl = computed(() => mediaThumbnailUrl(this.media()));
  protected readonly when = computed(() => formatDayAndTime(this.media().capturedAt));
  protected readonly duration = computed(() => formatDuration(this.media().durationSeconds));

  protected readonly comments = resource({
    params: () => ({ mediaId: this.media().id, groupId: this.groupId() }),
    loader: ({ params }) => this.service.forMedia(params.mediaId, params.groupId),
  });

  protected async send(body: string): Promise<void> {
    this.sending.set(true);
    try {
      await this.service.onMedia(this.media().id, this.groupId(), body);
      this.comments.reload();
      this.changed.emit();
    } finally {
      this.sending.set(false);
    }
  }

  protected async edit(comment: CommentView, body: string): Promise<void> {
    await this.service.edit(comment.id, body);
    this.comments.reload();
    this.changed.emit();
  }

  protected async remove(comment: CommentView): Promise<void> {
    if (!confirm('Apagar este comentário?')) {
      return;
    }
    await this.service.remove(comment.id);
    this.toast.success('Comentário apagado.');
    this.comments.reload();
    this.changed.emit();
  }
}
