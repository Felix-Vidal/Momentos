import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { Dialog } from '../../shared/ui/dialog';
import { ToastService } from '../../shared/ui/toast.service';
import { MediaService } from './media.service';

interface Pending {
  file: File;
  previewUrl: string | null;
  percent: number;
  status: 'waiting' | 'sending' | 'done' | 'error';
  error?: string;
}

/**
 * Envio de fotos e vídeos: arraste e solte ou escolha vários arquivos, com barra de
 * progresso por arquivo. As mídias ficam no perfil de quem enviou e aparecem no grupo
 * em que foram compartilhadas.
 */
@Component({
  selector: 'app-upload-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialog],
  template: `
    <app-dialog title="Enviar fotos e vídeos" (closed)="close()">
      <div class="flex flex-col gap-4">
        <label
          class="flex cursor-pointer flex-col items-center gap-2 rounded-box border-2 border-dashed
                 px-6 py-10 text-center transition"
          [class.border-brand]="dragging()"
          [class.bg-brand]="dragging()"
          [class.border-line]="!dragging()"
          [class.bg-surface]="!dragging()"
          (dragover)="onDragOver($event)"
          (dragleave)="dragging.set(false)"
          (drop)="onDrop($event)"
        >
          <span class="text-3xl" aria-hidden="true">⬆</span>
          <span class="text-sm">
            Arraste as fotos e vídeos aqui, ou <span class="text-brand-soft">escolha arquivos</span>
          </span>
          <span class="text-xs text-faint">
            Imagens até 25 MB e vídeos até 200 MB. A data vem dos metadados da foto.
          </span>
          <input
            type="file"
            multiple
            accept="image/*,video/*"
            class="hidden"
            (change)="onPick($event)"
          />
        </label>

        @if (items().length) {
          <ul class="flex flex-col gap-2">
            @for (item of items(); track item.file.name + item.file.lastModified) {
              <li class="flex items-center gap-3 rounded-xl border border-line bg-surface/40 p-2.5">
                @if (item.previewUrl) {
                  <img [src]="item.previewUrl" alt="" class="size-12 rounded-lg object-cover" />
                } @else {
                  <span class="grid size-12 place-items-center rounded-lg bg-base-soft text-lg" aria-hidden="true">▶</span>
                }

                <div class="flex min-w-0 flex-1 flex-col gap-1">
                  <span class="truncate text-sm">{{ item.file.name }}</span>
                  @if (item.status === 'error') {
                    <span class="text-xs text-danger">{{ item.error }}</span>
                  } @else {
                    <div class="h-1.5 overflow-hidden rounded-full bg-base-soft">
                      <div
                        class="h-full rounded-full bg-brand transition-[width] duration-200"
                        [style.width.%]="item.percent"
                      ></div>
                    </div>
                  }
                </div>

                <span class="shrink-0 text-xs text-faint">
                  @switch (item.status) {
                    @case ('done') { ✓ }
                    @case ('error') { ⚠ }
                    @case ('sending') { {{ item.percent }}% }
                    @default {
                      <button
                        type="button"
                        class="text-muted transition hover:text-danger"
                        (click)="discard(item)"
                        aria-label="Remover da lista"
                      >×</button>
                    }
                  }
                </span>
              </li>
            }
          </ul>
        }
      </div>

      <button
        slot="actions"
        type="button"
        class="rounded-full border border-line px-4 py-2 text-sm text-muted transition hover:text-ink"
        (click)="close()"
      >{{ finished() ? 'Fechar' : 'Cancelar' }}</button>

      <button
        slot="actions"
        type="button"
        [disabled]="!canSend()"
        class="rounded-full bg-brand px-5 py-2 text-sm font-medium text-base transition
               hover:bg-brand-soft active:scale-95 disabled:opacity-50"
        (click)="send()"
      >{{ sending() ? 'Enviando…' : 'Enviar ' + items().length + ' arquivo(s)' }}</button>
    </app-dialog>
  `,
})
export class UploadDialog {
  /** Grupo em que as mídias serão compartilhadas. */
  readonly groupId = input.required<string>();
  readonly closed = output<void>();
  readonly uploaded = output<number>();

  private readonly media = inject(MediaService);
  private readonly toast = inject(ToastService);

  protected readonly items = signal<Pending[]>([]);
  protected readonly dragging = signal(false);
  protected readonly sending = signal(false);
  protected readonly finished = signal(false);

  protected canSend(): boolean {
    return !this.sending() && this.items().some((item) => item.status === 'waiting');
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.add(event.dataTransfer?.files);
  }

  protected onPick(event: Event): void {
    this.add((event.target as HTMLInputElement).files);
    (event.target as HTMLInputElement).value = '';
  }

  protected discard(item: Pending): void {
    if (item.previewUrl) {
      URL.revokeObjectURL(item.previewUrl);
    }
    this.items.update((current) => current.filter((i) => i !== item));
  }

  protected async send(): Promise<void> {
    this.sending.set(true);
    let ok = 0;

    for (const item of this.items().filter((i) => i.status === 'waiting')) {
      this.patch(item, { status: 'sending', percent: 0 });
      try {
        await new Promise<void>((resolve, reject) => {
          this.media.uploadOne(item.file, [this.groupId()]).subscribe({
            next: (progress) => {
              this.patch(item, { percent: progress.percent });
              if (progress.done) {
                const rejected = progress.result?.rejected?.[0];
                if (rejected) {
                  this.patch(item, { status: 'error', error: rejected.reason });
                } else {
                  this.patch(item, { status: 'done', percent: 100 });
                  ok++;
                }
              }
            },
            error: (error) => reject(error),
            complete: () => resolve(),
          });
        });
      } catch (error) {
        this.patch(item, {
          status: 'error',
          error: (error as { message?: string }).message ?? 'Falha no envio.',
        });
      }
    }

    this.sending.set(false);
    this.finished.set(true);
    if (ok > 0) {
      this.toast.success(
        ok === 1 ? 'Mídia enviada!' : `${ok} mídias enviadas!`,
      );
      this.uploaded.emit(ok);
    }
  }

  protected close(): void {
    this.items().forEach((item) => {
      if (item.previewUrl) {
        URL.revokeObjectURL(item.previewUrl);
      }
    });
    this.closed.emit();
  }

  private add(files: FileList | null | undefined): void {
    if (!files?.length) {
      return;
    }
    const accepted = Array.from(files)
      .filter((file) => file.type.startsWith('image/') || file.type.startsWith('video/'))
      .map<Pending>((file) => ({
        file,
        previewUrl: file.type.startsWith('image/') ? URL.createObjectURL(file) : null,
        percent: 0,
        status: 'waiting',
      }));

    const ignored = files.length - accepted.length;
    if (ignored > 0) {
      this.toast.info(
        ignored === 1
          ? 'Um arquivo foi ignorado: só entram fotos e vídeos.'
          : `${ignored} arquivos foram ignorados: só entram fotos e vídeos.`,
      );
    }
    this.items.update((current) => [...current, ...accepted]);
    this.finished.set(false);
  }

  private patch(item: Pending, changes: Partial<Pending>): void {
    this.items.update((current) =>
      current.map((candidate) => (candidate === item ? { ...candidate, ...changes } : candidate)),
    );
  }
}
