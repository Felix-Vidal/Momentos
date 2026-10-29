import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiError } from '../../core/api/error.interceptor';
import { FieldError } from '../../shared/ui/field-error';
import { ToastService } from '../../shared/ui/toast.service';
import { GroupsService } from './groups.service';

@Component({
  selector: 'app-create-group',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, FieldError],
  template: `
    <section class="mx-auto flex w-full max-w-lg flex-col gap-6 py-4">
      <header class="flex flex-col gap-1">
        <h1 class="text-2xl font-semibold">Criar grupo</h1>
        <p class="text-sm text-muted">Você será o primeiro administrador.</p>
      </header>

      <form class="flex flex-col gap-5" [formGroup]="form" (ngSubmit)="submit()">
        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">Nome do grupo</span>
          <input
            formControlName="name"
            placeholder="Sociedade da Roleta"
            class="rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                   transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          />
        </label>

        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">Descrição</span>
          <textarea
            formControlName="description"
            rows="4"
            maxlength="500"
            placeholder="Como o grupo começou, de onde vem o nome…"
            class="resize-none rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                   transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          ></textarea>
        </label>

        <fieldset class="flex flex-col gap-2">
          <legend class="mb-1 text-sm text-muted">Quem pode encontrar o grupo</legend>
          @for (option of visibilities; track option.value) {
            <label
              class="flex cursor-pointer items-start gap-3 rounded-xl border border-line bg-surface/40 p-3
                     transition hover:border-brand/60"
              [class.border-brand]="form.controls.visibility.value === option.value"
            >
              <input
                type="radio"
                formControlName="visibility"
                [value]="option.value"
                class="mt-1 accent-brand"
              />
              <span class="flex flex-col gap-0.5">
                <span class="text-sm">{{ option.label }}</span>
                <span class="text-xs text-faint">{{ option.hint }}</span>
              </span>
            </label>
          }
        </fieldset>

        <label class="flex items-center gap-3 rounded-xl border border-line bg-surface/40 p-3">
          <input type="checkbox" formControlName="roulette" class="accent-brand" />
          <span class="flex flex-col gap-0.5">
            <span class="text-sm">Tema roleta</span>
            <span class="text-xs text-faint">Coloca uma roleta animada no topo do grupo.</span>
          </span>
        </label>

        <app-field-error [message]="failure()" />

        <button
          type="submit"
          [disabled]="sending()"
          class="rounded-full bg-brand px-5 py-2.5 text-sm font-medium text-base transition
                 hover:bg-brand-soft active:scale-95 disabled:opacity-60"
        >
          {{ sending() ? 'Criando…' : 'Criar grupo' }}
        </button>
      </form>
    </section>
  `,
})
export class CreateGroupPage {
  private readonly service = inject(GroupsService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly sending = signal(false);
  protected readonly failure = signal<string | null>(null);

  protected readonly visibilities = [
    {
      value: 'OPEN' as const,
      label: 'Aberto',
      hint: 'Aparece na busca pública e qualquer pessoa pode pedir para entrar.',
    },
    {
      value: 'PRIVATE' as const,
      label: 'Privado',
      hint: 'Não aparece em lugar nenhum: só entra quem for convidado.',
    },
  ];

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    description: [''],
    visibility: ['OPEN' as 'OPEN' | 'PRIVATE', [Validators.required]],
    roulette: [false],
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.failure.set('O nome do grupo precisa de ao menos 2 caracteres.');
      return;
    }
    this.sending.set(true);
    this.failure.set(null);
    const { name, description, visibility, roulette } = this.form.getRawValue();
    try {
      const group = await this.service.create({
        name,
        description: description || null,
        visibility,
        theme: roulette ? 'ROULETTE' : 'DEFAULT',
      });
      this.toast.success('Grupo criado! Convide a turma.');
      await this.router.navigate(['/g', group.slug]);
    } catch (error) {
      this.failure.set((error as ApiError).message ?? 'Não foi possível criar o grupo.');
    } finally {
      this.sending.set(false);
    }
  }
}
