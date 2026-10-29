import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { UserProfile } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { FieldError } from '../../shared/ui/field-error';
import { ToastService } from '../../shared/ui/toast.service';

@Component({
  selector: 'app-profile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, FieldError],
  template: `
    <section class="mx-auto flex w-full max-w-lg flex-col gap-6 py-6">
      <header class="flex flex-col gap-1">
        <h1 class="text-2xl font-semibold">Meu perfil</h1>
        <p class="text-sm text-muted">{{ user()?.email }}</p>
      </header>

      <form class="flex flex-col gap-4" [formGroup]="form" (ngSubmit)="save()">
        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">Nome</span>
          <input
            formControlName="name"
            class="rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                   transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          />
        </label>

        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">Bio</span>
          <textarea
            formControlName="bio"
            rows="3"
            maxlength="280"
            placeholder="Conte em uma linha quem você é no grupo."
            class="resize-none rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                   transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          ></textarea>
        </label>

        <app-field-error [message]="failure()" />

        <div class="flex items-center gap-3">
          <button
            type="submit"
            [disabled]="saving()"
            class="rounded-full bg-brand px-5 py-2.5 text-sm font-medium text-base transition
                   hover:bg-brand-soft active:scale-95 disabled:opacity-60"
          >
            {{ saving() ? 'Salvando…' : 'Salvar' }}
          </button>
          <button
            type="button"
            class="rounded-full border border-line px-5 py-2.5 text-sm text-muted transition
                   hover:border-danger hover:text-danger"
            (click)="auth.logout()"
          >Sair da conta</button>
        </div>
      </form>
    </section>
  `,
})
export class ProfilePage {
  protected readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly user = this.auth.currentUser;
  protected readonly saving = signal(false);
  protected readonly failure = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: [this.user()?.name ?? '', [Validators.required, Validators.minLength(2)]],
    bio: [this.user()?.bio ?? ''],
  });

  protected async save(): Promise<void> {
    if (this.form.invalid) {
      this.failure.set('O nome deve ter ao menos 2 caracteres.');
      return;
    }
    this.saving.set(true);
    this.failure.set(null);
    try {
      const updated = await firstValueFrom(
        this.http.patch<UserProfile>('/api/users/me', this.form.getRawValue()),
      );
      this.auth.patchUser(updated);
      this.toast.success('Perfil atualizado.');
    } catch {
      this.failure.set('Não foi possível salvar agora.');
    } finally {
      this.saving.set(false);
    }
  }
}
