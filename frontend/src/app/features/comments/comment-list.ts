import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommentView } from '../../core/api/timeline.models';
import { formatDayAndTime } from '../../core/util/date.util';
import { Avatar } from '../../shared/ui/avatar';

@Component({
  selector: 'app-comment-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Avatar],
  template: `
    @if (comments().length) {
      <ul class="flex flex-col gap-3">
        @for (comment of comments(); track comment.id) {
          <li class="flex items-start gap-2.5">
            <app-avatar
              [name]="comment.author.name"
              [mediaId]="comment.author.avatarMediaId"
              size="sm"
            />
            <div class="flex min-w-0 flex-1 flex-col gap-1">
              <div class="flex flex-wrap items-center gap-2">
                <span class="text-sm font-medium">{{ comment.author.name }}</span>
                <span class="text-xs text-faint">{{ when(comment.createdAt) }}</span>
                @if (comment.edited) {
                  <span class="text-xs text-faint">· editado</span>
                }
              </div>

              @if (editingId() === comment.id) {
                <div class="flex items-end gap-2">
                  <textarea
                    [(ngModel)]="draft"
                    rows="2"
                    maxlength="1000"
                    class="flex-1 resize-none rounded-xl border border-line bg-surface/60 px-3 py-2 text-sm
                           outline-none focus:border-brand focus:ring-2 focus:ring-brand/30"
                  ></textarea>
                  <button
                    type="button"
                    class="rounded-full bg-brand px-3 py-1.5 text-xs font-medium text-base"
                    (click)="confirmEdit(comment)"
                  >Salvar</button>
                  <button
                    type="button"
                    class="rounded-full border border-line px-3 py-1.5 text-xs text-muted"
                    (click)="editingId.set(null)"
                  >Cancelar</button>
                </div>
              } @else {
                <p class="text-sm whitespace-pre-wrap text-muted">{{ comment.body }}</p>
                @if (comment.canEdit || comment.canDelete) {
                  <div class="flex items-center gap-2">
                    @if (comment.canEdit) {
                      <button
                        type="button"
                        class="text-xs text-faint transition hover:text-brand-soft"
                        (click)="startEdit(comment)"
                      >Editar</button>
                    }
                    @if (comment.canDelete) {
                      <button
                        type="button"
                        class="text-xs text-faint transition hover:text-danger"
                        (click)="removed.emit(comment)"
                      >Apagar</button>
                    }
                  </div>
                }
              }
            </div>
          </li>
        }
      </ul>
    } @else {
      <p class="text-sm text-faint">Seja o primeiro a comentar.</p>
    }
  `,
})
export class CommentList {
  readonly comments = input.required<CommentView[]>();
  readonly edited = output<{ comment: CommentView; body: string }>();
  readonly removed = output<CommentView>();

  protected readonly editingId = signal<string | null>(null);
  protected draft = '';
  protected readonly when = formatDayAndTime;

  protected startEdit(comment: CommentView): void {
    this.draft = comment.body;
    this.editingId.set(comment.id);
  }

  protected confirmEdit(comment: CommentView): void {
    const body = this.draft.trim();
    if (body && body !== comment.body) {
      this.edited.emit({ comment, body });
    }
    this.editingId.set(null);
  }
}
