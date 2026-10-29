import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-default-hero',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="relative flex flex-col gap-2 overflow-hidden rounded-box border border-line
             bg-gradient-to-br from-surface/80 to-base px-6 py-8"
    >
      <div
        class="pointer-events-none absolute -right-20 -top-20 size-64 rounded-full bg-brand/15 blur-3xl"
        aria-hidden="true"
      ></div>
      <h1 class="relative text-3xl font-semibold">{{ name() }}</h1>
      @if (description()) {
        <p class="relative max-w-2xl text-sm text-muted">{{ description() }}</p>
      }
    </div>
  `,
})
export class DefaultHero {
  readonly name = input.required<string>();
  readonly description = input<string | null>(null);
}
