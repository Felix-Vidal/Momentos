import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { MediaView } from '../../core/api/media.models';
import { MomentBlock, MomentView, TimelineMedia } from '../../core/api/timeline.models';
import { StompService } from '../../core/realtime/stomp.service';
import { dayKey, formatDay } from '../../core/util/date.util';
import { injectGroup } from '../groups/group-context';
import { MediaService } from '../media/media.service';
import { EmptyState } from '../../shared/ui/empty-state';
import { MediaSkeleton } from '../../shared/ui/media-skeleton';
import { Spinner } from '../../shared/ui/spinner';
import { ToastService } from '../../shared/ui/toast.service';
import { Lightbox } from '../media/lightbox';
import { MomentCommentsDialog } from '../comments/moment-comments.dialog';
import { CreateMomentDialog } from './create-moment.dialog';
import { DateNav } from './date-nav';
import { MediaCard } from './media-card';
import { MomentSection } from './moment-section';
import { TimelineStore } from './timeline.store';

/** Um Momento, ou as mídias soltas de um mesmo dia. */
type TimelineSection =
  | { kind: 'MOMENT'; id: string; block: MomentBlock }
  | { kind: 'DAY'; id: string; day: string; label: string; items: TimelineMedia[] };

/**
 * Aba principal do grupo: fotos e vídeos em ordem cronológica, com os que caem no
 * intervalo de um Momento agrupados sob o título dele.
 *
 * <p>As mídias que não pertencem a nenhum Momento são reunidas por dia, de modo que
 * a data apareça uma vez só por bloco, e não uma vez por foto.
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
    MediaSkeleton,
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

      @if (store.isLoading() && !store.entries().length) {
        <app-media-skeleton [count]="6" />
      } @else if (store.isEmpty()) {
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
        @for (section of sections(); track section.id) {
          <li class="flex flex-col gap-3">
            @if (section.kind === 'MOMENT') {
              <app-moment-section
                [block]="section.block"
                [isAdmin]="isAdmin()"
                (edit)="startEditing($event)"
                (remove)="confirmRemove($event)"
                (openComments)="openMomentComments($event)"
              >
                <span slot="title">{{ section.block.moment.title }}</span>

                <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  @for (item of section.block.media; track item.media.id) {
                    <app-media-card [item]="item" (opened)="openMedia($event)" />
                  }
                </div>
              </app-moment-section>
            } @else {
              <div class="flex flex-col gap-3">
                <header class="flex items-center gap-3">
                  <p class="text-xs tracking-wide text-faint uppercase">{{ section.label }}</p>
                  <span class="h-px flex-1 bg-line/60" aria-hidden="true"></span>
                  <span class="text-xs text-faint">
                    {{ section.items.length }}
                    {{ section.items.length === 1 ? 'mídia' : 'mídias' }}
                  </span>
                </header>
                <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  @for (item of section.items; track item.media.id) {
                    <app-media-card [item]="item" (opened)="openMedia($event)" />
                  }
                </div>
              </div>
            }
          </li>
        }
      </ol>

      @if (store.isLoading() && store.entries().length) {
        <app-spinner label="Carregando mais…" />
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

  /**
   * Agrupa mídias soltas vizinhas do mesmo dia sob um único cabeçalho.
   *
   * <p>O agrupamento é por vizinhança, não global: se um Momento acontece no meio do
   * dia, o que vem antes e o que vem depois continuam em blocos separados, e a ordem
   * cronológica da página é preservada.
   */
  protected readonly sections = computed<TimelineSection[]>(() => {
    const sections: TimelineSection[] = [];

    for (const entry of this.store.entries()) {
      if (entry.kind === 'MOMENT' && entry.moment) {
        sections.push({ kind: 'MOMENT', id: entry.moment.moment.id, block: entry.moment });
        continue;
      }
      if (!entry.media) {
        continue;
      }

      const day = dayKey(entry.at);
      const previous = sections.at(-1);
      if (previous?.kind === 'DAY' && previous.day === day) {
        previous.items.push(entry.media);
      } else {
        sections.push({
          kind: 'DAY',
          // O mesmo dia pode voltar depois de um Momento, então o identificador
          // carrega a primeira mídia do bloco para nunca se repetir no `track`.
          id: `dia-${day}-${entry.media.media.id}`,
          day,
          label: formatDay(entry.at),
          items: [entry.media],
        });
      }
    }
    return sections;
  });

  /** Recarregamento agrupado: várias mensagens seguidas viram uma recarga só. */
  private reloadTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    // Abre a linha do tempo quando o grupo chega e recarrega quando entram mídias novas.
    //
    // O carregamento roda em `untracked`: a parte síncrona de `open()` lê sinais do
    // próprio store (o grupo atual e o indicador de carregamento) e os escreve logo
    // em seguida. Sem isolar, o effect passaria a depender deles e se redisparia em
    // laço, limpando a lista a cada volta — a tela ficaria eternamente no esqueleto.
    effect(() => {
      const groupId = this.context.groupId();
      this.media.revision();
      if (groupId) {
        untracked(() => void this.store.open(groupId));
      }
    });

    // Comentário novo de qualquer pessoa aparece sem recarregar a página.
    effect((onCleanup) => {
      const groupId = this.context.groupId();
      if (!groupId) {
        return;
      }
      const subscription = untracked(() =>
        this.stomp.comments(groupId).subscribe(() => this.scheduleReload()),
      );
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
