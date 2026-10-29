import { HttpClient, HttpEventType } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { MediaView, UploadResult } from '../../core/api/media.models';

export interface UploadProgress {
  /** 0 a 100 enquanto envia; o backend ainda processa depois de chegar a 100. */
  percent: number;
  done: boolean;
  result?: UploadResult;
}

@Injectable({ providedIn: 'root' })
export class MediaService {
  private readonly http = inject(HttpClient);

  /** Avança quando algo muda em mídias, para a linha do tempo recarregar. */
  readonly revision = signal(0);

  /**
   * Envia um arquivo e relata o progresso. Cada arquivo vai em uma requisição
   * própria: assim a barra é individual e uma falha não derruba o lote inteiro.
   */
  uploadOne(file: File, groupIds: string[]): Observable<UploadProgress> {
    const form = new FormData();
    form.append('files', file, file.name);

    const params = new URLSearchParams();
    groupIds.forEach((id) => params.append('groupIds', id));
    // O fuso do navegador é o que interpreta a hora local gravada no EXIF.
    params.set('timeZone', Intl.DateTimeFormat().resolvedOptions().timeZone);

    return new Observable<UploadProgress>((subscriber) => {
      const subscription = this.http
        .post<UploadResult>(`/api/media?${params.toString()}`, form, {
          reportProgress: true,
          observe: 'events',
        })
        .subscribe({
          next: (event) => {
            if (event.type === HttpEventType.UploadProgress) {
              const percent = event.total ? Math.round((event.loaded / event.total) * 100) : 0;
              subscriber.next({ percent, done: false });
            } else if (event.type === HttpEventType.Response) {
              this.revision.update((n) => n + 1);
              subscriber.next({ percent: 100, done: true, result: event.body ?? undefined });
              subscriber.complete();
            }
          },
          error: (error) => subscriber.error(error),
        });
      return () => subscription.unsubscribe();
    });
  }

  mine(limit = 60): Promise<MediaView[]> {
    return firstValueFrom(this.http.get<MediaView[]>('/api/media/mine', { params: { limit } }));
  }

  byMemberInGroup(userId: string, groupId: string): Promise<MediaView[]> {
    return firstValueFrom(
      this.http.get<MediaView[]>(`/api/users/${userId}/media`, { params: { groupId } }),
    );
  }

  async update(mediaId: string, data: { capturedAt?: string; caption?: string }): Promise<MediaView> {
    const updated = await firstValueFrom(this.http.patch<MediaView>(`/api/media/${mediaId}`, data));
    this.revision.update((n) => n + 1);
    return updated;
  }

  async remove(mediaId: string): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`/api/media/${mediaId}`));
    this.revision.update((n) => n + 1);
  }

  async unshare(mediaId: string, groupId: string): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`/api/media/${mediaId}/groups/${groupId}`));
    this.revision.update((n) => n + 1);
  }

  async setAvatar(mediaId: string): Promise<void> {
    await firstValueFrom(this.http.put(`/api/users/me/avatar/${mediaId}`, {}));
  }
}
