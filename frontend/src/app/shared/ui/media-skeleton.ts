import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Espaços pulsantes no lugar das mídias, enquanto a primeira página carrega. */
@Component({
  selector: 'app-media-skeleton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" aria-hidden="true">
      @for (slot of slots(); track $index) {
        <div class="aspect-square animate-pulse rounded-box bg-surface/60"></div>
      }
    </div>
  `,
})
export class MediaSkeleton {
  readonly count = input(6);

  protected slots(): number[] {
    return Array.from({ length: this.count() }, (_, index) => index);
  }
}
