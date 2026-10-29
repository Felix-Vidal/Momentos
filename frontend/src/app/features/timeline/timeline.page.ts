import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { MediaView } from '../../core/api/media.models';
import { MomentBlock, MomentView } from '../../core/api/timeline.models';
import { StompService } from '../../core/realtime/stomp.service';
import { formatDay } from '../../core/util/date.util';
import { injectGroup } from '../groups/group-context';
import { MediaService } from '../media/media.service';
import { EmptyState } from '../../shared/ui/empty-state';
import { Spinner } from '../../shared/ui/spinner';
import { ToastService } from '../../shared/ui/toast.service';
import { Lightbox } from '../media/lightbox';
import { MomentCommentsDialog } from '../comments/moment-comments.dialog';
import { CreateMomentDialog } from './create-moment.dialog';
import { DateNav } from './date-nav';
import { MediaCard } from './media-card';
import { MomentSection } from './moment-section';
import { TimelineStore } from './timeline.store';

/**
 * Aba principal do grupo: fotos e vídeos em ordem cronológica, com os que caem no
 * intervalo de um Momento agrupados sob o título dele.
 */
@Component({
  selector: 'app-timeline',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [TimelineStore],
  imports: [
    MediaCard,
    MomentSection,
    DateNav,
    CreateMomentDialog,
    MomentCommentsDialog,
    Lightbox,
    EmptyState,
    Spinner,
  ],
  template: `
    <div class="flex flex-col gap-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <app-date-nav
          class="flex-1"
          [calendar]="store.calendar()"
          [selectedDay]="store.selectedDay()"
          (daySelected)="store.selectDay($event)"
        />
        <button
          type="button"
          class="shrink-0 rounded-full border border-line px-4 py-2 text-sm text-muted transition
                 hover:border-brand hover:text-ink"
          (click)="startCreating()"
        >Criar momento</button>
      </div>

      @if (store.isEmpty()) {
        <app-empty-state
          icon="◷"
          title="A linha do tempo está vazia"
          [hint]="
            store.selectedDay()
              ? 'Nenhuma mídia nesse dia. Escolha outro dia ou veja tudo.'
              : 'Envie as primeiras fotos e vídeos para começar a contar a história do grupo.'
          "
        />
      }

      <ol class="flex flex-col gap-10">
        @for (entry of store.entries(); track entry.kind + (entry.moment?.moment.id ?? entry.media?.media.id)) {
          <li class="flex flex-col gap-3">
            @if (entry.kind === 'MOMENT' && entry.moment) {
              <app-moment-section
                [block]="entry.moment"
                [isAdmin]="isAdmin()"
                (edit)="startEditing($event)"
                (remove)="confirmRemove($event)"
                (openComments)="openMomentComments($event)"
              >
                <span slot="title">{{ entry.moment.moment.title }}</span>

                <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  @for (item of entry.moment.media; track item.media.id) {
                    <app-media-card [item]="item" (opened)="openMedia($event)" />
                  }
                </div>
              </app-moment-section>
            } @else if (entry.media) {
              <div class="flex flex-col gap-2">
                <p class="text-xs tracking-wide text-faint uppercase">{{ day(entry.at) }}</p>
                <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  <app-media-card [item]="entry.media" (opened)="openMedia($event)" />
                </div>
              </div>
            }
          </li>
        }
      </ol>

      @if (store.isLoading()) {
        <app-spinner label="Carregando a linha do tempo…" />
      } @else if (store.hasMore()) {
        <button
          type="button"
          class="mx-auto rounded-full border border-line px-5 py-2.5 text-sm text-muted transition
                 hover:border-brand hover:text-ink"
          (click)="store.loadNext()"
        >Carregar mais</button>
      }

      @if (creating()) {
        <app-create-moment-dialog
          [moment]="editingMoment()"
          [suggestedDay]="store.selectedDay()"
          (closed)="stopCreating()"
          (saved)="stopCreating()"
        />
      }

      @if (viewing(); as media) {
        <app-lightbox
          [media]="media"
          [groupId]="groupId()!"
          (closed)="viewing.set(null)"
          (changed)="store.reload()"
        />
      }

      @if (momentComments(); as moment) {
        <app-moment-comments-dialog
          [moment]="moment"
          (closed)="momentComments.set(null)"
          (changed)="store.reload()"
        />
      }
    </div>
  `,
})
export class TimelinePage {
  protected readonly store = inject(TimelineStore);
  private readonly media = inject(MediaService);
  private readonly toast = inject(ToastService);
  private readonly context = injectGroup();

  private readonly stomp = inject(StompService);

  protected readonly creating = signal(false);
  protected readonly editingMoment = signal<MomentView | null>(null);
  protected readonly viewing = signal<MediaView | null>(null);
  protected readonly momentComments = signal<MomentView | null>(null);
  protected readonly isAdmin = computed(() => this.context.group.value()?.myRole === 'ADMIN');
  protected readonly groupId = this.context.groupId;

  protected readonly day = formatDay;

  /** Recarregamento agrupado: várias mensagens seguidas viram uma recarga só. */
  private reloadTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    // Abre a linha do tempo quando o grupo chega e recarrega quando entram mídias novas.
    effect(() => {
      const groupId = this.context.groupId();
      this.media.revision();
      if (groupId) {
        void this.store.open(groupId);
      }
    });

    // Comentário novo de qualquer pessoa aparece sem recarregar a página.
    effect((onCleanup) => {
      const groupId = this.context.groupId();
      if (!groupId) {
        return;
      }
      const subscription = this.stomp.comments(groupId).subscribe(() => this.scheduleReload());
      onCleanup(() => subscription.unsubscribe());
    });
  }

  private scheduleReload(): void {
    clearTimeout(this.reloadTimer);
    this.reloadTimer = setTimeout(() => void this.store.reload(), 400);
  }

  protected startCreating(): void {
    this.editingMoment.set(null);
    this.creating.set(true);
  }

  protected startEditing(block: MomentBlock): void {
    this.editingMoment.set(block.moment);
    this.creating.set(true);
  }

  protected stopCreating(): void {
    this.creating.set(false);
    this.editingMoment.set(null);
  }

  protected async confirmRemove(block: MomentBlock): Promise<void> {
    const title = block.moment.title;
    if (!confirm(`Apagar o momento "${title}"? As mídias continuam no grupo, soltas.`)) {
      return;
    }
    await this.store.deleteMoment(block.moment.id);
    this.toast.success(`Momento "${title}" apagado.`);
  }

  protected openMedia(media: MediaView): void {
    this.viewing.set(media);
  }

  protected openMomentComments(block: MomentBlock): void {
    this.momentComments.set(block.moment);
  }
}
