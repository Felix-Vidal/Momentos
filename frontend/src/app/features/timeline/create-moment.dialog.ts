import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError } from '../../core/api/error.interceptor';
import { MomentView } from '../../core/api/timeline.models';
import { isoToLocalInput, localInputToIso } from '../../core/util/date.util';
import { Dialog } from '../../shared/ui/dialog';
import { FieldError } from '../../shared/ui/field-error';
import { ToastService } from '../../shared/ui/toast.service';
import { TimelineStore } from './timeline.store';

/**
 * Cria ou edita um Momento: um título para um intervalo de data e hora.
 * Os atalhos cobrem os casos comuns (um dia, uma noite que vira o dia).
 */
@Component({
  selector: 'app-create-moment-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, Dialog, FieldError],
  template: `
    <app-dialog [title]="editing() ? 'Editar momento' : 'Criar momento'" (closed)="closed.emit()">
      <form class="flex flex-col gap-4" [formGroup]="form" (ngSubmit)="submit()">
        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">Título</span>
          <input
            formControlName="title"
            placeholder="Corujão da LetalCompany"
            class="rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                   transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          />
        </label>

        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">Descrição (opcional)</span>
          <textarea
            formControlName="description"
            rows="2"
            maxlength="500"
            placeholder="O que aconteceu nessa madrugada…"
            class="resize-none rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm
                   outline-none transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          ></textarea>
        </label>

        <div class="grid gap-3 sm:grid-cols-2">
          <label class="flex flex-col gap-1.5">
            <span class="text-sm text-muted">Começa em</span>
            <input
              type="datetime-local"
              formControlName="startsAt"
              class="rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                     transition focus:border-brand focus:ring-2 focus:ring-brand/30"
            />
          </label>
          <label class="flex flex-col gap-1.5">
            <span class="text-sm text-muted">Termina em</span>
            <input
              type="datetime-local"
              formControlName="endsAt"
              class="rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                     transition focus:border-brand focus:ring-2 focus:ring-brand/30"
            />
          </label>
        </div>

        <div class="flex flex-wrap gap-2">
          <span class="self-center text-xs text-faint">Atalhos:</span>
          <button
            type="button"
            class="rounded-full border border-line px-3 py-1 text-xs text-muted transition
                   hover:border-brand hover:text-ink"
            (click)="applyPreset('day')"
          >Um dia inteiro</button>
          <button
            type="button"
            class="rounded-full border border-line px-3 py-1 text-xs text-muted transition
                   hover:border-brand hover:text-ink"
            (click)="applyPreset('night')"
          >Uma noite (22:00 → 06:00)</button>
          <button
            type="button"
            class="rounded-full border border-line px-3 py-1 text-xs text-muted transition
                   hover:border-brand hover:text-ink"
            (click)="applyPreset('afternoon')"
          >Uma tarde (12:00 → 18:00)</button>
        </div>

        <p class="text-xs text-faint">
          Toda foto ou vídeo capturado nesse intervalo passa a aparecer sob este título.
        </p>

        <app-field-error [message]="failure()" />
      </form>

      <button
        slot="actions"
        type="button"
        class="rounded-full border border-line px-4 py-2 text-sm text-muted transition hover:text-ink"
        (click)="closed.emit()"
      >Cancelar</button>

      <button
        slot="actions"
        type="button"
        [disabled]="saving()"
        class="rounded-full bg-brand px-5 py-2 text-sm font-medium text-base transition
               hover:bg-brand-soft active:scale-95 disabled:opacity-60"
        (click)="submit()"
      >{{ saving() ? 'Salvando…' : editing() ? 'Salvar' : 'Criar momento' }}</button>
    </app-dialog>
  `,
})
export class CreateMomentDialog {
  /** Quando informado, o diálogo edita esse Momento em vez de criar um novo. */
  readonly moment = input<MomentView | null>(null);
  /** Dia sugerido (YYYY-MM-DD) quando o usuário cria a partir de um dia da timeline. */
  readonly suggestedDay = input<string | null>(null);

  readonly closed = output<void>();
  readonly saved = output<MomentView>();

  private readonly store = inject(TimelineStore);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly saving = signal(false);
  protected readonly failure = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(2)]],
    description: [''],
    startsAt: ['', [Validators.required]],
    endsAt: ['', [Validators.required]],
  });

  constructor() {
    const existing = this.moment();
    if (existing) {
      this.form.patchValue({
        title: existing.title,
        description: existing.description ?? '',
        startsAt: isoToLocalInput(existing.startsAt),
        endsAt: isoToLocalInput(existing.endsAt),
      });
    } else {
      this.applyPreset('day');
    }
  }

  protected editing(): boolean {
    return this.moment() !== null;
  }

  protected applyPreset(preset: 'day' | 'night' | 'afternoon'): void {
    const base = this.suggestedDay() ? new Date(`${this.suggestedDay()}T12:00:00`) : new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    const dayOf = (date: Date) =>
      `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

    const nextDay = new Date(base);
    nextDay.setDate(base.getDate() + 1);

    const presets = {
      day: [`${dayOf(base)}T00:00`, `${dayOf(nextDay)}T00:00`],
      // Uma madrugada atravessa a meia-noite: termina no dia seguinte.
      night: [`${dayOf(base)}T22:00`, `${dayOf(nextDay)}T06:00`],
      afternoon: [`${dayOf(base)}T12:00`, `${dayOf(base)}T18:00`],
    } as const;

    const [startsAt, endsAt] = presets[preset];
    this.form.patchValue({ startsAt, endsAt });
  }

  protected async submit(): Promise<void> {
    this.failure.set(null);
    if (this.form.invalid) {
      this.failure.set('Informe o título e o intervalo do momento.');
      return;
    }
    const value = this.form.getRawValue();
    if (new Date(value.endsAt) <= new Date(value.startsAt)) {
      this.failure.set('O fim precisa ser depois do início.');
      return;
    }

    this.saving.set(true);
    try {
      const payload = {
        title: value.title,
        description: value.description || null,
        startsAt: localInputToIso(value.startsAt),
        endsAt: localInputToIso(value.endsAt),
      };
      const existing = this.moment();
      const result = existing
        ? await this.store.updateMoment(existing.id, payload)
        : await this.store.createMoment(payload);

      this.toast.success(existing ? 'Momento atualizado.' : `Momento "${result.title}" criado.`);
      this.saved.emit(result);
    } catch (error) {
      this.failure.set((error as ApiError).message ?? 'Não foi possível salvar o momento.');
    } finally {
      this.saving.set(false);
    }
  }
}
