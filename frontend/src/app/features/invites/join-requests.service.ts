import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface JoinRequestView {
  id: string;
  groupId: string;
  userId: string;
  userName: string;
  userAvatarMediaId: string | null;
  message: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class JoinRequestsService {
  private readonly http = inject(HttpClient);

  ask(groupId: string, message?: string): Promise<void> {
    return firstValueFrom(
      this.http.post<void>(`/api/groups/${groupId}/join-requests`, { message: message ?? null }),
    );
  }

  pending(groupId: string): Promise<JoinRequestView[]> {
    return firstValueFrom(
      this.http.get<JoinRequestView[]>(`/api/groups/${groupId}/join-requests`),
    );
  }

  approve(groupId: string, requestId: string): Promise<void> {
    return firstValueFrom(
      this.http.post<void>(`/api/groups/${groupId}/join-requests/${requestId}/approve`, {}),
    );
  }

  reject(groupId: string, requestId: string): Promise<void> {
    return firstValueFrom(
      this.http.post<void>(`/api/groups/${groupId}/join-requests/${requestId}/reject`, {}),
    );
  }
}
