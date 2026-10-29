import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { GroupRole } from '../../core/api/api.models';

export interface InviteView {
  id: string;
  groupId: string;
  /** Vem apenas na criação: depois, o servidor só guarda o hash. */
  token: string | null;
  invitedEmail: string | null;
  role: GroupRole;
  expiresAt: string;
  maxUses: number;
  uses: number;
  revoked: boolean;
  createdAt: string;
}

export interface InvitePreview {
  groupId: string | null;
  groupName: string | null;
  groupDescription: string | null;
  coverMediaId: string | null;
  invitedBy: string | null;
  valid: boolean;
  alreadyMember: boolean;
  reason: string | null;
}

export interface NewInvite {
  email?: string | null;
  role?: GroupRole;
  expiresInHours?: number;
  maxUses?: number;
}

@Injectable({ providedIn: 'root' })
export class InvitesService {
  private readonly http = inject(HttpClient);

  create(groupId: string, invite: NewInvite): Promise<InviteView> {
    return firstValueFrom(this.http.post<InviteView>(`/api/groups/${groupId}/invites`, invite));
  }

  list(groupId: string): Promise<InviteView[]> {
    return firstValueFrom(this.http.get<InviteView[]>(`/api/groups/${groupId}/invites`));
  }

  revoke(groupId: string, inviteId: string): Promise<void> {
    return firstValueFrom(
      this.http.delete<void>(`/api/groups/${groupId}/invites/${inviteId}`),
    );
  }

  preview(token: string): Promise<InvitePreview> {
    return firstValueFrom(this.http.get<InvitePreview>(`/api/invites/${token}`));
  }

  accept(token: string): Promise<{ groupId: string }> {
    return firstValueFrom(
      this.http.post<{ groupId: string }>(`/api/invites/${token}/accept`, {}),
    );
  }

  mine(): Promise<InviteView[]> {
    return firstValueFrom(this.http.get<InviteView[]>('/api/invites/mine'));
  }

  /** Link que o administrador compartilha com a turma. */
  linkFor(token: string): string {
    return `${location.origin}/convite/${token}`;
  }
}
