/** Tipos espelhando os DTOs da API. */

export type GroupVisibility = 'OPEN' | 'PRIVATE';
export type GroupRole = 'ADMIN' | 'MEMBER';
export type GroupTheme = 'DEFAULT' | 'ROULETTE';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
  hasMore: boolean;
}

export interface GroupSummary {
  id: string;
  slug: string;
  name: string;
  description: string | null;
  coverMediaId: string | null;
  visibility: GroupVisibility;
  theme: GroupTheme;
  myRole: GroupRole;
  memberCount: number;
}

export interface GroupDetail extends GroupSummary {
  createdAt: string;
}

export interface PublicGroupCard {
  id: string;
  slug: string;
  name: string;
  description: string | null;
  coverMediaId: string | null;
  memberCount: number;
  member: boolean;
  requested: boolean;
}

export interface GroupMemberView {
  userId: string;
  name: string;
  bio: string | null;
  avatarMediaId: string | null;
  role: GroupRole;
  joinedAt: string;
}
