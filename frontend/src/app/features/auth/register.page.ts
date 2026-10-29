import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/api/error.interceptor';
import { AuthService } from '../../core/auth/auth.service';
import { FieldError } from '../../shared/ui/field-error';

@Component({
  selector: 'app-register',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, FieldError],
  template: `
    <section class="mx-auto flex w-full max-w-sm flex-col gap-6 py-10">
      <header class="flex flex-col gap-1 text-center">
        <h1 class="text-2xl font-semibold">Criar conta</h1>
        <p class="text-sm text-muted">
          Depois é só entrar em um grupo — por convite ou pedindo para participar.
        </p>
      </header>

      <form class="flex flex-col gap-4" [formGroup]="form" (ngSubmit)="submit()">
        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">Nome</span>
          <input
            formControlName="name"
            autocomplete="name"
            class="rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                   transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          />
          <app-field-error [message]="errors()['name']" />
        </label>

        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">E-mail</span>
          <input
            type="email"
            formControlName="email"
            autocomplete="email"
            class="rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                   transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          />
          <app-field-error [message]="errors()['email']" />
        </label>

        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">Senha</span>
          <input
            type="password"
            formControlName="password"
            autocomplete="new-password"
            class="rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                   transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          />
          <span class="text-xs text-faint">Pelo menos 8 caracteres.</span>
          <app-field-error [message]="errors()['password']" />
        </label>

        <app-field-error [message]="failure()" />

        <button
          type="submit"
          [disabled]="sending()"
          class="mt-1 rounded-full bg-brand px-5 py-2.5 text-sm font-medium text-base transition
                 hover:bg-brand-soft active:scale-95 disabled:opacity-60"
        >
          {{ sending() ? 'Criando…' : 'Criar conta' }}
        </button>
      </form>

      <p class="text-center text-sm text-muted">
        Já tem conta?
        <a routerLink="/entrar" class="text-brand-soft underline-offset-4 hover:underline">Entrar</a>
      </p>
    </section>
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly sending = signal(false);
  protected readonly failure = signal<string | null>(null);
  protected readonly errors = signal<Record<string, string>>({});

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  protected async submit(): Promise<void> {
    this.errors.set({});
    this.failure.set(null);
    if (this.form.invalid) {
      this.failure.set('Confira os campos antes de continuar.');
      return;
    }
    this.sending.set(true);
    try {
      await this.auth.register(this.form.getRawValue());
      await this.router.navigateByUrl('/grupos');
    } catch (error) {
      const apiError = error as ApiError;
      // O backend devolve erro por campo em `fields`; o resto vira mensagem geral.
      if (apiError.fields?.length) {
        this.errors.set(
          Object.fromEntries(apiError.fields.map((f) => [f.field, f.message])),
        );
      }
      this.failure.set(apiError.fields?.length ? null : (apiError.message ?? 'Não foi possível criar a conta.'));
    } finally {
      this.sending.set(false);
    }
  }
}
