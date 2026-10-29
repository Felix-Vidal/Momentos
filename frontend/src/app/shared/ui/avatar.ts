import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Foto de perfil redonda; sem foto, mostra as iniciais. */
@Component({
  selector: 'app-avatar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (mediaId()) {
      <img
        [src]="'/api/media/' + mediaId() + '/thumbnail'"
        [alt]="'Foto de ' + name()"
        loading="lazy"
        class="size-full rounded-full object-cover"
      />
    } @else {
      <span class="font-medium" aria-hidden="true">{{ initials() }}</span>
      <span class="sr-only">{{ name() }}</span>
    }
  `,
  host: {
    class:
      'grid shrink-0 place-items-center overflow-hidden rounded-full bg-surface text-brand-soft',
    '[class]': 'sizeClass()',
  },
})
export class Avatar {
  readonly name = input.required<string>();
  readonly mediaId = input<string | null | undefined>();
  readonly size = input<'sm' | 'md' | 'lg'>('md');

  protected readonly sizeClass = computed(
    () =>
      ({
        sm: 'size-8 text-xs',
        md: 'size-10 text-sm',
        lg: 'size-20 text-xl',
      })[this.size()],
  );

  protected readonly initials = computed(() =>
    this.name()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]!.toUpperCase())
      .join(''),
  );
}
