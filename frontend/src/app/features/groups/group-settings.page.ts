import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { FieldError } from '../../shared/ui/field-error';
import { ToastService } from '../../shared/ui/toast.service';
import { InviteDialog } from '../invites/invite.dialog';
import { JoinRequestsPanel } from '../invites/join-requests-panel';
import { injectGroup } from './group-context';
import { GroupsService } from './groups.service';

@Component({
  selector: 'app-group-settings',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, FieldError, InviteDialog, JoinRequestsPanel],
  template: `
    <section class="flex max-w-lg flex-col gap-6">
      @if (groupId(); as id) {
        <app-join-requests-panel [groupId]="id" />

        <div class="flex flex-col gap-2 rounded-box border border-line bg-surface/40 p-4">
          <p class="text-sm">Convidar gente para o grupo</p>
          <p class="text-xs text-faint">
            Gere um link para compartilhar no grupo de mensagens ou convide alguém pelo e-mail.
            Ninguém entra sem convite ou aprovação.
          </p>
          <button
            type="button"
            class="mt-1 self-start rounded-full bg-brand px-4 py-2 text-sm font-medium text-base
                   transition hover:bg-brand-soft active:scale-95"
            (click)="inviting.set(true)"
          >Convidar</button>
        </div>
      }

      <form class="flex flex-col gap-5" [formGroup]="form" (ngSubmit)="save()">
        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">Nome do grupo</span>
          <input
            formControlName="name"
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
            class="resize-none rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                   transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          ></textarea>
        </label>

        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">Visibilidade</span>
          <select
            formControlName="visibility"
            class="rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                   transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          >
            <option value="OPEN">Aberto — aparece na busca pública</option>
            <option value="PRIVATE">Privado — só por convite</option>
          </select>
        </label>

        <label class="flex flex-col gap-1.5">
          <span class="text-sm text-muted">Tema</span>
          <select
            formControlName="theme"
            class="rounded-xl border border-line bg-surface/60 px-3.5 py-2.5 text-sm outline-none
                   transition focus:border-brand focus:ring-2 focus:ring-brand/30"
          >
            <option value="DEFAULT">Padrão</option>
            <option value="ROULETTE">Roleta animada</option>
          </select>
        </label>

        <app-field-error [message]="failure()" />

        <button
          type="submit"
          [disabled]="saving()"
          class="self-start rounded-full bg-brand px-5 py-2.5 text-sm font-medium text-base transition
                 hover:bg-brand-soft active:scale-95 disabled:opacity-60"
        >{{ saving() ? 'Salvando…' : 'Salvar alterações' }}</button>
      </form>

      <div class="flex flex-col gap-2 rounded-box border border-line/70 p-4">
        <p class="text-sm">Sair do grupo</p>
        <p class="text-xs text-faint">
          Suas mídias continuam suas, mas deixam de aparecer na linha do tempo deste grupo.
          Se você for o único administrador, promova outra pessoa antes.
        </p>
        <button
          type="button"
          class="mt-1 self-start rounded-full border border-line px-4 py-2 text-xs text-muted transition
                 hover:border-danger hover:text-danger"
          (click)="leave()"
        >Sair do grupo</button>
      </div>

      @if (inviting() && groupId()) {
        <app-invite-dialog [groupId]="groupId()!" (closed)="inviting.set(false)" />
      }
    </section>
  `,
})
export class GroupSettingsPage {
  private readonly service = inject(GroupsService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  private readonly context = injectGroup();

  protected readonly saving = signal(false);
  protected readonly failure = signal<string | null>(null);
  protected readonly inviting = signal(false);
  protected readonly groupId = this.context.groupId;

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    description: [''],
    visibility: ['OPEN' as 'OPEN' | 'PRIVATE'],
    theme: ['DEFAULT' as 'DEFAULT' | 'ROULETTE'],
  });

  constructor() {
    // Preenche o formulário quando o grupo termina de carregar.
    effect(() => {
      const group = this.context.group.value();
      if (group) {
        this.form.patchValue(
          {
            name: group.name,
            description: group.description ?? '',
            visibility: group.visibility,
            theme: group.theme,
          },
          { emitEvent: false },
        );
      }
    });
  }

  protected async save(): Promise<void> {
    const id = this.context.groupId();
    if (!id || this.form.invalid) {
      this.failure.set('O nome do grupo precisa de ao menos 2 caracteres.');
      return;
    }
    this.saving.set(true);
    this.failure.set(null);
    const value = this.form.getRawValue();
    try {
      const updated = await this.service.update(id, {
        name: value.name,
        description: value.description || null,
        visibility: value.visibility,
        theme: value.theme,
      });
      this.toast.success('Configurações salvas.');
      // O slug não muda ao renomear, mas a navegação garante o estado atualizado.
      await this.router.navigate(['/g', updated.slug, 'configuracoes']);
    } catch {
      this.failure.set('Não foi possível salvar agora.');
    } finally {
      this.saving.set(false);
    }
  }

  protected async leave(): Promise<void> {
    const id = this.context.groupId();
    const myId = this.auth.currentUser()?.id;
    if (!id || !myId || !confirm('Tem certeza que quer sair deste grupo?')) {
      return;
    }
    try {
      await this.service.removeMember(id, myId);
      this.toast.success('Você saiu do grupo.');
      await this.router.navigate(['/grupos']);
    } catch {
      // A mensagem do backend (ex.: único administrador) já aparece como toast.
    }
  }
}
