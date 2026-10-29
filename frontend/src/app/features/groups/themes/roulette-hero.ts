import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Hero do tema "roleta": uma roleta em SVG que gira devagar e acelera no hover.
 * Homenagem ao ritual do grupo que decide quem leva as bandejas no RU.
 */
@Component({
  selector: 'app-roulette-hero',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="group relative flex flex-col items-center gap-4 overflow-hidden rounded-box
             border border-line bg-gradient-to-b from-surface/80 to-base px-6 py-10 text-center"
    >
      <div
        class="pointer-events-none absolute -top-24 left-1/2 size-72 -translate-x-1/2 rounded-full
               bg-brand/20 blur-3xl"
        aria-hidden="true"
      ></div>

      <svg viewBox="0 0 100 100" class="wheel size-28 drop-shadow-lg" aria-hidden="true">
        @for (slice of slices; track slice.start) {
          <path [attr.d]="slice.path" [attr.fill]="slice.color" />
        }
        <circle cx="50" cy="50" r="12" fill="var(--color-base)" />
        <circle cx="50" cy="50" r="12" fill="none" stroke="var(--color-brand-soft)" stroke-width="1.5" />
        <circle cx="50" cy="50" r="3" fill="var(--color-brand-soft)" />
      </svg>

      <div class="relative flex flex-col gap-1">
        <h1 class="text-3xl font-semibold">{{ name() }}</h1>
        @if (description()) {
          <p class="mx-auto max-w-xl text-sm text-muted">{{ description() }}</p>
        }
      </div>
    </div>
  `,
  styles: `
    .wheel {
      animation: spin 18s linear infinite;
      transform-origin: center;
    }

    .group:hover .wheel {
      animation-duration: 3s;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `,
})
export class RouletteHero {
  readonly name = input.required<string>();
  readonly description = input<string | null>(null);

  /** Oito fatias alternando as cores da marca, montadas uma vez. */
  protected readonly slices = Array.from({ length: 8 }, (_, index) => {
    const start = (index * 360) / 8;
    const end = ((index + 1) * 360) / 8;
    const palette = [
      'var(--color-brand)',
      'var(--color-brand-strong)',
      'var(--color-brand-soft)',
      'var(--color-accent)',
    ];
    return { start, path: wedge(start, end), color: palette[index % palette.length] };
  });
}

/** Caminho SVG de uma fatia de pizza entre dois ângulos. */
function wedge(startDeg: number, endDeg: number): string {
  const point = (deg: number) => {
    const rad = ((deg - 90) * Math.PI) / 180;
    return `${(50 + 46 * Math.cos(rad)).toFixed(2)} ${(50 + 46 * Math.sin(rad)).toFixed(2)}`;
  };
  return `M 50 50 L ${point(startDeg)} A 46 46 0 0 1 ${point(endDeg)} Z`;
}
