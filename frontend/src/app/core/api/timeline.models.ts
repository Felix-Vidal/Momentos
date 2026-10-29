import { MediaView, UserSummary } from './media.models';

export interface MomentView {
  id: string;
  groupId: string;
  title: string;
  description: string | null;
  startsAt: string;
  endsAt: string;
  mediaCount: number;
  createdBy: UserSummary;
  createdAt: string;
}

export interface CommentView {
  id: string;
  groupId: string;
  mediaId: string | null;
  momentId: string | null;
  body: string;
  author: UserSummary;
  createdAt: string;
  updatedAt: string;
  edited: boolean;
  canEdit: boolean;
  canDelete: boolean;
  /** Mídia a que o comentário se refere, para mostrar a miniatura no feed do Momento. */
  mediaThumbnail: string | null;
}

export interface MomentBlock {
  moment: MomentView;
  media: MediaView[];
  commentCount: number;
  commentFeed: CommentView[];
}

export interface TimelineEntry {
  kind: 'MOMENT' | 'MEDIA';
  at: string;
  moment: MomentBlock | null;
  media: MediaView | null;
}

export interface TimelineCalendar {
  months: { month: string; count: number; days: { date: string; count: number }[] }[];
  total: number;
}
