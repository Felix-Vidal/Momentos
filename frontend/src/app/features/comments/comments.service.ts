import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CommentView } from '../../core/api/timeline.models';

@Injectable({ providedIn: 'root' })
export class CommentsService {
  private readonly http = inject(HttpClient);

  forMedia(mediaId: string, groupId: string): Promise<CommentView[]> {
    return firstValueFrom(
      this.http.get<CommentView[]>(`/api/media/${mediaId}/comments`, { params: { groupId } }),
    );
  }

  forMoment(momentId: string): Promise<CommentView[]> {
    return firstValueFrom(this.http.get<CommentView[]>(`/api/moments/${momentId}/comments`));
  }

  onMedia(mediaId: string, groupId: string, body: string): Promise<CommentView> {
    return firstValueFrom(
      this.http.post<CommentView>(
        `/api/media/${mediaId}/comments`,
        { body },
        { params: { groupId } },
      ),
    );
  }

  onMoment(momentId: string, body: string): Promise<CommentView> {
    return firstValueFrom(
      this.http.post<CommentView>(`/api/moments/${momentId}/comments`, { body }),
    );
  }

  edit(commentId: string, body: string): Promise<CommentView> {
    return firstValueFrom(this.http.patch<CommentView>(`/api/comments/${commentId}`, { body }));
  }

  remove(commentId: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/comments/${commentId}`));
  }
}
