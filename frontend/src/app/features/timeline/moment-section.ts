import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { MediaView } from '../../core/api/media.models';
import { MomentBlock } from '../../core/api/timeline.models';
import { AuthService } from '../../core/auth/auth.service';
import { formatInterval } from '../../core/util/date.util';
import { InViewDirective } from '../../shared/directives/in-view.directive';

/**
 * Bloco de um Momento na linha do tempo: o título, o intervalo e as mídias que
 * caíram nele. O conteúdo projetado traz os cartões de mídia e a caixa de
 * comentários do título.
 */
@Component({
  selector: 'app-moment-section',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InViewDirective],
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
          <h2 class="relative inline-flex w-fit items-center gap-2 font-display text-xl">
            <span
              class="h-5 w-1 rounded-full bg-gradient-to-b from-brand to-accent"
              aria-hidden="true"
            ></span>
            <ng-content select="[slot=title]" />
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

  private readonly auth = inject(AuthService);

  protected readonly interval = computed(() =>
    formatInterval(this.block().moment.startsAt, this.block().moment.endsAt),
  );

  /** Quem criou o Momento pode editá-lo; administradores do grupo também. */
  protected readonly canEdit = computed(
    () => this.isAdmin() || this.block().moment.createdBy.id === this.auth.currentUser()?.id,
  );
}
