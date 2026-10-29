import { ChangeDetectionStrategy, Component, inject, input, output, resource, signal } from '@angular/core';
import { CommentView, MomentView } from '../../core/api/timeline.models';
import { formatInterval } from '../../core/util/date.util';
import { Dialog } from '../../shared/ui/dialog';
import { Spinner } from '../../shared/ui/spinner';
import { ToastService } from '../../shared/ui/toast.service';
import { CommentForm } from './comment-form';
import { CommentList } from './comment-list';
import { CommentsService } from './comments.service';

/**
 * Conversa completa de um Momento: o que foi dito no conjunto, com a possibilidade
 * de comentar no Momento como um todo.
 */
@Component({
  selector: 'app-moment-comments-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialog, CommentList, CommentForm, Spinner],
  template: `
    <app-dialog [title]="moment().title" (closed)="closed.emit()">
      <div class="flex flex-col gap-4">
        <p class="text-xs text-faint">{{ interval() }}</p>
        @if (moment().description) {
          <p class="text-sm text-muted">{{ moment().description }}</p>
        }

        @if (comments.isLoading()) {
          <app-spinner label="Carregando a conversa…" />
        } @else {
          <app-comment-list
            [comments]="comments.value() ?? []"
            (edited)="edit($event.comment, $event.body)"
            (removed)="remove($event)"
          />
        }
      </div>

      <app-comment-form
        slot="actions"
        class="w-full"
        placeholder="Comentar no momento…"
        [sending]="sending()"
        (submitted)="send($event)"
      />
    </app-dialog>
  `,
})
export class MomentCommentsDialog {
  readonly moment = input.required<MomentView>();
  readonly closed = output<void>();
  readonly changed = output<void>();

  private readonly service = inject(CommentsService);
  private readonly toast = inject(ToastService);

  protected readonly sending = signal(false);

  protected readonly comments = resource({
    params: () => this.moment().id,
    loader: ({ params }) => this.service.forMoment(params),
  });

  protected interval(): string {
    return formatInterval(this.moment().startsAt, this.moment().endsAt);
  }

  protected async send(body: string): Promise<void> {
    this.sending.set(true);
    try {
      await this.service.onMoment(this.moment().id, body);
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
