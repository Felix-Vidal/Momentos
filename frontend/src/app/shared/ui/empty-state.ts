import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col items-center gap-2 rounded-box border border-dashed border-line px-6 py-12 text-center">
      <span class="text-3xl" aria-hidden="true">{{ icon() }}</span>
      <p class="font-display text-lg">{{ title() }}</p>
      @if (hint()) {
        <p class="max-w-sm text-sm text-muted">{{ hint() }}</p>
      }
      <ng-content />
    </div>
  `,
})
export class EmptyState {
  readonly icon = input('◌');
  readonly title = input.required<string>();
  readonly hint = input<string | null>(null);
}
