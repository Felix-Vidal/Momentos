import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CursorPage } from '../../core/api/api.models';
import {
  MomentView,
  TimelineCalendar,
  TimelineEntry,
} from '../../core/api/timeline.models';
import { browserTimeZone } from '../../core/util/date.util';

export interface MomentForm {
  title: string;
  description?: string | null;
  startsAt: string;
  endsAt: string;
}

/**
 * Estado da linha do tempo de um grupo: páginas acumuladas por cursor, filtro por
 * dia e as operações de Momento.
 */
@Injectable()
export class TimelineStore {
  private readonly http = inject(HttpClient);

  private readonly groupId = signal<string | null>(null);
  private readonly pages = signal<TimelineEntry[]>([]);
  private readonly cursor = signal<string | null>(null);
  private readonly more = signal(false);
  private readonly loading = signal(false);
  private readonly day = signal<string | null>(null);

  readonly entries = this.pages.asReadonly();
  readonly hasMore = this.more.asReadonly();
  readonly isLoading = this.loading.asReadonly();
  readonly selectedDay = this.day.asReadonly();
  readonly isEmpty = computed(() => !this.loading() && this.pages().length === 0);

  readonly calendar = signal<TimelineCalendar | null>(null);

  async open(groupId: string): Promise<void> {
    this.groupId.set(groupId);
    await this.reload();
  }

  /** Recomeça do início, mantendo o filtro de dia atual. */
  async reload(): Promise<void> {
    this.pages.set([]);
    this.cursor.set(null);
    this.more.set(false);
    await this.loadNext();
    await this.loadCalendar();
  }

  async loadNext(): Promise<void> {
    const groupId = this.groupId();
    if (!groupId || this.loading()) {
      return;
    }
    this.loading.set(true);
    try {
      const page = await this.fetch(groupId);
      this.pages.update((current) => [...current, ...page.items]);
      this.cursor.set(page.nextCursor);
      this.more.set(page.hasMore);
    } finally {
      this.loading.set(false);
    }
  }

  async selectDay(date: string | null): Promise<void> {
    this.day.set(date);
    await this.reload();
  }

  async createMoment(form: MomentForm): Promise<MomentView> {
    const groupId = this.groupId();
    const created = await firstValueFrom(
      this.http.post<MomentView>(`/api/groups/${groupId}/moments`, form),
    );
    await this.reload();
    return created;
  }

  async updateMoment(momentId: string, form: Partial<MomentForm>): Promise<MomentView> {
    const updated = await firstValueFrom(
      this.http.patch<MomentView>(`/api/moments/${momentId}`, form),
    );
    await this.reload();
    return updated;
  }

  async deleteMoment(momentId: string): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`/api/moments/${momentId}`));
    await this.reload();
  }

  private fetch(groupId: string): Promise<CursorPage<TimelineEntry>> {
    const day = this.day();
    if (day) {
      return firstValueFrom(
        this.http.get<CursorPage<TimelineEntry>>(
          `/api/groups/${groupId}/timeline/day/${day}`,
          { params: { timeZone: browserTimeZone() } },
        ),
      );
    }
    const params: Record<string, string | number> = { limit: 24 };
    const cursor = this.cursor();
    if (cursor) {
      params['cursor'] = cursor;
    }
    return firstValueFrom(
      this.http.get<CursorPage<TimelineEntry>>(`/api/groups/${groupId}/timeline`, { params }),
    );
  }

  private async loadCalendar(): Promise<void> {
    const groupId = this.groupId();
    if (!groupId) {
      return;
    }
    const calendar = await firstValueFrom(
      this.http.get<TimelineCalendar>(`/api/groups/${groupId}/timeline/calendar`, {
        params: { timeZone: browserTimeZone() },
      }),
    );
    this.calendar.set(calendar);
  }
}
