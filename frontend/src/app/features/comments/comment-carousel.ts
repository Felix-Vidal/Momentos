import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  OnDestroy,
  signal,
  viewChild,
} from '@angular/core';
import { animate } from 'motion';
import { CommentView } from '../../core/api/timeline.models';
import { Avatar } from '../../shared/ui/avatar';

/** Tempo base por comentário e folga por tamanho do texto. */
const BASE_MS = 3500;
const PER_CHAR_MS = 22;
const MIN_MS = 3000;
const MAX_MS = 6000;
const MAX_CHARS = 120;

/**
 * Caixa retangular que aparece na base da mídia (ou abaixo do título de um Momento)
 * e mostra os comentários um por vez, em loop, com avatar, nome e texto.
 *
 * <p>Visual de vidro: fundo semitransparente com desfoque, cantos arredondados e
 * sombra suave, de modo a permanecer legível sobre qualquer foto. A troca é animada
 * com fade e leve deslize para cima; textos longos ficam mais tempo na tela e são
 * truncados com "…" — o texto completo está no lightbox.
 */
@Component({
  selector: 'app-comment-carousel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar],
  template: `
    <div
      class="pointer-events-none absolute z-30 flex justify-center transition-all duration-300
             ease-[var(--ease-soft)]"
      [class]="placementClass()"
      [class.opacity-0]="!visible()"
      [class.translate-y-2]="!visible()"
      [class.opacity-100]="visible()"
      [class.translate-y-0]="visible()"
      [attr.aria-hidden]="!visible()"
    >
      <div
        class="glass rounded-2xl px-3.5 py-2.5"
        [class]="boxClass()"
        role="status"
        aria-live="polite"
      >
        @if (current(); as comment) {
          <div class="flex items-start gap-2.5" #slide>
            @if (comment.mediaThumbnail && showMediaThumbnail()) {
              <img
                [src]="'/api/media/' + comment.mediaThumbnail + '/thumbnail'"
                alt=""
                class="size-9 shrink-0 rounded-lg object-cover"
              />
            }
            <app-avatar [name]="comment.author.name" [mediaId]="comment.author.avatarMediaId" size="sm" />
            <div class="flex min-w-0 flex-1 flex-col">
              <span class="truncate text-xs font-medium text-brand-soft">
                {{ comment.author.name }}
              </span>
              <p class="text-sm leading-snug text-ink">{{ truncated() }}</p>
            </div>
          </div>

          @if (comments().length > 1) {
            <div class="mt-2 flex items-center justify-center gap-1" aria-hidden="true">
              @for (dot of comments(); track dot.id; let i = $index) {
                <span
                  class="h-1 rounded-full transition-all duration-300"
                  [class]="i === index() ? 'w-5 bg-brand-soft' : 'w-1 bg-ink/25'"
                ></span>
              }
            </div>
          }
        } @else {
          <p class="text-center text-sm text-muted">{{ emptyMessage() }}</p>
        }
      </div>
    </div>
  `,
})
export class CommentCarousel implements OnDestroy {
  readonly comments = input.required<CommentView[]>();
  /** Enquanto falso, a caixa fica escondida e o ciclo pausado. */
  readonly visible = input(false);
  /** No feed de um Momento, cada comentário mostra a miniatura da mídia comentada. */
  readonly showMediaThumbnail = input(false);
  readonly emptyMessage = input('Seja o primeiro a comentar');
  /**
   * `media` ancora a caixa na base da foto ou vídeo (80% da largura, com limite);
   * `below` a coloca logo abaixo do título de um Momento.
   */
  readonly placement = input<'media' | 'below'>('media');

  private readonly slide = viewChild<ElementRef<HTMLElement>>('slide');
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly index = signal(0);
  private timer?: ReturnType<typeof setTimeout>;

  protected readonly current = computed(() => this.comments()[this.index()] ?? null);

  /** Largura proporcional à mídia, sem cobrir a foto inteira. */
  protected readonly placementClass = computed(() =>
    this.placement() === 'media' ? 'inset-x-0 bottom-0 px-3 pb-3' : 'left-0 top-full pt-2',
  );

  protected readonly boxClass = computed(() =>
    this.placement() === 'media' ? 'w-[80%] max-w-md' : 'w-[min(26rem,80vw)]',
  );

  protected readonly truncated = computed(() => {
    const body = this.current()?.body ?? '';
    return body.length > MAX_CHARS ? `${body.slice(0, MAX_CHARS).trimEnd()}…` : body;
  });

  constructor() {
    // Liga e desliga o ciclo conforme a caixa aparece ou some.
    effect(() => {
      const visible = this.visible();
      const total = this.comments().length;

      this.stop();
      if (!visible || total === 0) {
        if (!visible) {
          this.index.set(0);
        }
        return;
      }
      this.animateSlide();
      if (total > 1) {
        this.schedule();
      }
    });
  }

  ngOnDestroy(): void {
    this.stop();
  }

  /** Comentários longos ficam mais tempo na tela, dentro de um limite. */
  private durationOf(comment: CommentView | null): number {
    const length = Math.min(comment?.body.length ?? 0, MAX_CHARS);
    return Math.min(MAX_MS, Math.max(MIN_MS, BASE_MS + length * PER_CHAR_MS));
  }

  private schedule(): void {
    this.timer = setTimeout(() => {
      this.index.update((current) => (current + 1) % this.comments().length);
      this.animateSlide();
      this.schedule();
    }, this.durationOf(this.current()));
  }

  private animateSlide(): void {
    if (this.prefersReducedMotion()) {
      return;
    }
    // O elemento é recriado a cada troca; esperar um tick garante a referência nova.
    setTimeout(() => {
      const element = this.slide()?.nativeElement;
      if (!element) {
        return;
      }
      try {
        animate(
          element,
          { opacity: [0, 1], transform: ['translateY(8px)', 'translateY(0)'] },
          { duration: 0.38, ease: [0.22, 1, 0.36, 1] },
        );
      } catch {
        // Sem suporte a animação, o comentário simplesmente aparece — a caixa
        // nunca deve quebrar por causa do efeito visual.
      }
    });
  }

  private prefersReducedMotion(): boolean {
    return (
      this.host.nativeElement.ownerDocument.defaultView?.matchMedia(
        '(prefers-reduced-motion: reduce)',
      ).matches ?? false
    );
  }

  private stop(): void {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = undefined;
    }
  }
}
