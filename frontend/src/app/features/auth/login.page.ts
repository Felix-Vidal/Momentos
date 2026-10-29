import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/api/error.interceptor';
import { AuthService } from '../../core/auth/auth.service';
import { FieldError } from '../../shared/ui/field-error';

@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, FieldError],
  template: `
    <section class="mx-auto flex w-full max-w-sm flex-col gap-6 py-10">
      <header class="flex flex-col gap-1 text-center">
        <h1 class="text-2xl font-semibold">Entrar</h1>
        <p class="text-sm text-muted">Bem-vindo de volta à linha do tempo.</p>
      </header>

      <form class="flex flex-col gap-4" [formGroup]="form" (ngSubmit)="submit()">
        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">E-mail</span>
          <input
            type="email"
            formControlName="email"
            autocomplete="email"
            class="rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                   transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          />
        </label>

        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">Senha</span>
          <input
            type="password"
            formControlName="password"
            autocomplete="current-password"
            class="rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                   transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          />
        </label>

        <app-field-error [message]="failure()" />

        <button
          type="submit"
          [disabled]="sending()"
          class="mt-1 rounded-full bg-brand px-5 py-2.5 text-sm font-medium text-base transition
                 hover:bg-brand-soft active:scale-95 disabled:opacity-60"
        >
          {{ sending() ? 'Entrando…' : 'Entrar' }}
        </button>
      </form>

      <p class="text-center text-sm text-muted">
        Ainda não tem conta?
        <a routerLink="/criar-conta" class="text-brand-soft underline-offset-4 hover:underline">
          Criar conta
        </a>
      </p>
    </section>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly sending = signal(false);
  protected readonly failure = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.failure.set('Informe e-mail e senha.');
      return;
    }
    this.sending.set(true);
    this.failure.set(null);
    try {
      await this.auth.login(this.form.getRawValue());
      const target = new URLSearchParams(location.search).get('retornar') ?? '/grupos';
      await this.router.navigateByUrl(target);
    } catch (error) {
      this.failure.set((error as ApiError).message ?? 'Não foi possível entrar.');
    } finally {
      this.sending.set(false);
    }
  }
}
