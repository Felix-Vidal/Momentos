import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  GroupDetail,
  GroupMemberView,
  GroupRole,
  GroupSummary,
  GroupTheme,
  GroupVisibility,
  PageResponse,
  PublicGroupCard,
} from '../../core/api/api.models';

export interface GroupForm {
  name: string;
  description?: string | null;
  visibility?: GroupVisibility;
  theme?: GroupTheme;
}

@Injectable({ providedIn: 'root' })
export class GroupsService {
  private readonly http = inject(HttpClient);

  /** Incrementado a cada mudança, para as telas recarregarem o que depende de grupos. */
  readonly revision = signal(0);

  mine(): Promise<GroupSummary[]> {
    return firstValueFrom(this.http.get<GroupSummary[]>('/api/groups'));
  }

  discover(query: string, page = 0): Promise<PageResponse<PublicGroupCard>> {
    return firstValueFrom(
      this.http.get<PageResponse<PublicGroupCard>>('/api/groups/public', {
        params: { q: query, page },
      }),
    );
  }

  bySlug(slug: string): Promise<GroupDetail> {
    return firstValueFrom(this.http.get<GroupDetail>(`/api/groups/by-slug/${slug}`));
  }

  members(groupId: string): Promise<GroupMemberView[]> {
    return firstValueFrom(this.http.get<GroupMemberView[]>(`/api/groups/${groupId}/members`));
  }

  async create(form: GroupForm): Promise<GroupDetail> {
    const created = await firstValueFrom(this.http.post<GroupDetail>('/api/groups', form));
    this.touch();
    return created;
  }

  async update(groupId: string, form: Partial<GroupForm>): Promise<GroupDetail> {
    const updated = await firstValueFrom(
      this.http.patch<GroupDetail>(`/api/groups/${groupId}`, form),
    );
    this.touch();
    return updated;
  }

  async changeRole(groupId: string, userId: string, role: GroupRole): Promise<GroupMemberView> {
    const member = await firstValueFrom(
      this.http.patch<GroupMemberView>(`/api/groups/${groupId}/members/${userId}`, { role }),
    );
    this.touch();
    return member;
  }

  async removeMember(groupId: string, userId: string): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`/api/groups/${groupId}/members/${userId}`));
    this.touch();
  }

  private touch(): void {
    this.revision.update((n) => n + 1);
  }
}
