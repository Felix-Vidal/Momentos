import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { Avatar } from '../../shared/ui/avatar';

@Component({
  selector: 'app-comment-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Avatar],
  template: `
    <form class="flex items-end gap-2" (ngSubmit)="submit()">
      <app-avatar
        [name]="me()?.name ?? 'Você'"
        [mediaId]="me()?.avatarMediaId"
        size="sm"
        class="mb-1"
      />
      <label class="flex-1">
        <span class="sr-only">Escreva um comentário</span>
        <textarea
          [(ngModel)]="text"
          name="body"
          rows="1"
          maxlength="1000"
          [placeholder]="placeholder()"
          class="w-full resize-none rounded-2xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm
                 outline-none transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          (keydown.enter)="onEnter($event)"
        ></textarea>
      </label>
      <button
        type="submit"
        [disabled]="sending() || !text.trim()"
        class="mb-1 shrink-0 rounded-full bg-brand px-4 py-2 text-sm font-medium text-base
               transition hover:bg-brand-soft active:scale-95 disabled:opacity-50"
      >{{ sending() ? '…' : 'Enviar' }}</button>
    </form>
  `,
})
export class CommentForm {
  readonly placeholder = input('Escreva um comentário…');
  readonly sending = input(false);
  readonly submitted = output<string>();

  private readonly auth = inject(AuthService);
  protected readonly me = this.auth.currentUser;
  protected text = '';

  protected submit(): void {
    const body = this.text.trim();
    if (!body) {
      return;
    }
    this.submitted.emit(body);
    this.text = '';
  }

  /** Enter envia; Shift+Enter quebra linha. */
  protected onEnter(event: Event): void {
    const keyboard = event as KeyboardEvent;
    if (!keyboard.shiftKey) {
      event.preventDefault();
      this.submit();
    }
  }
}
