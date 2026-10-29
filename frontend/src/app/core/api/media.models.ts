export type MediaKind = 'IMAGE' | 'VIDEO';
export type CapturedAtSource = 'EXIF' | 'UPLOAD' | 'MANUAL';

export interface UserSummary {
  id: string;
  name: string;
  avatarMediaId: string | null;
}

export interface MediaView {
  id: string;
  type: MediaKind;
  mimeType: string;
  width: number | null;
  height: number | null;
  durationSeconds: string | null;
  capturedAt: string;
  capturedAtSource: CapturedAtSource;
  caption: string | null;
  hasThumbnail: boolean;
  owner: UserSummary;
}

export interface UploadResult {
  uploaded: MediaView[];
  rejected: { fileName: string; reason: string }[];
}

/** URLs dos arquivos de uma mídia (servidos pelo backend, só para quem tem acesso). */
export function mediaFileUrl(mediaId: string): string {
  return `/api/media/${mediaId}/file`;
}

export function mediaThumbnailUrl(media: Pick<MediaView, 'id' | 'hasThumbnail'>): string {
  return media.hasThumbnail ? `/api/media/${media.id}/thumbnail` : mediaFileUrl(media.id);
}
