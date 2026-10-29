import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { TimelineCalendar } from '../../core/api/timeline.models';
import { formatMonth } from '../../core/util/date.util';

/** Navegação por data: escolher um mês e, dentro dele, pular para um dia. */
@Component({
  selector: 'app-date-nav',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (calendar()?.months?.length) {
      <nav class="flex flex-col gap-2 rounded-box border border-line bg-surface/40 p-3">
        <div class="flex flex-wrap items-center gap-1.5">
          <span class="mr-1 text-xs text-faint">Ir para:</span>
          @for (month of calendar()!.months; track month.month) {
            <button
              type="button"
              class="rounded-full px-3 py-1 text-xs transition"
              [class]="
                openMonth() === month.month
                  ? 'bg-brand text-base'
                  : 'border border-line text-muted hover:border-brand hover:text-ink'
              "
              (click)="toggleMonth(month.month)"
            >{{ label(month.month) }} <span class="opacity-60">{{ month.count }}</span></button>
          }
          @if (selectedDay()) {
            <button
              type="button"
              class="ml-auto rounded-full border border-line px-3 py-1 text-xs text-muted
                     transition hover:border-danger hover:text-danger"
              (click)="daySelected.emit(null)"
            >Ver tudo</button>
          }
        </div>

        @if (days().length) {
          <div class="flex flex-wrap gap-1.5 border-t border-line/60 pt-2">
            @for (day of days(); track day.date) {
              <button
                type="button"
                class="rounded-lg px-2.5 py-1 text-xs transition"
                [class]="
                  selectedDay() === day.date
                    ? 'bg-brand-soft text-base'
                    : 'bg-base-soft text-muted hover:bg-surface hover:text-ink'
                "
                (click)="daySelected.emit(day.date)"
              >{{ dayNumber(day.date) }} <span class="opacity-60">{{ day.count }}</span></button>
            }
          </div>
        }
      </nav>
    }
  `,
})
export class DateNav {
  readonly calendar = input<TimelineCalendar | null>(null);
  readonly selectedDay = input<string | null>(null);
  readonly daySelected = output<string | null>();

  protected readonly openMonth = signal<string | null>(null);

  protected readonly days = computed(() => {
    const month = this.openMonth();
    if (!month) {
      return [];
    }
    return this.calendar()?.months.find((m) => m.month === month)?.days ?? [];
  });

  protected label(month: string): string {
    return formatMonth(month);
  }

  protected dayNumber(date: string): string {
    return date.slice(8, 10);
  }

  protected toggleMonth(month: string): void {
    this.openMonth.update((current) => (current === month ? null : month));
  }
}
