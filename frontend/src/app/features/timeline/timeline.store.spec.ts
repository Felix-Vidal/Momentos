import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
  TestRequest,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TimelineStore } from './timeline.store';

const emptyCalendar = { months: [], total: 0 };
const emptyPage = { items: [], nextCursor: null, hasMore: false };

function mediaEntry(id: string, at: string) {
  return {
    kind: 'MEDIA' as const,
    at,
    moment: null,
    media: { media: { id, capturedAt: at }, commentCount: 0, comments: [] },
  };
}

describe('TimelineStore', () => {
  let store: TimelineStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TimelineStore, provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(TimelineStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /**
   * As chamadas da store são sequenciais (uma depende da anterior), então o teste
   * atende o que estiver pendente em cada tick até a operação terminar, e devolve
   * as requisições vistas para inspeção.
   */
  async function settle(
    operation: Promise<unknown>,
    bodies: { timeline?: unknown; calendar?: unknown; other?: unknown } = {},
  ): Promise<TestRequest[]> {
    const seen: TestRequest[] = [];
    let running = true;
    operation.finally(() => (running = false));

    for (let tick = 0; tick < 12 && (running || http.match(() => true).length); tick++) {
      await new Promise((resolve) => setTimeout(resolve, 0));
      for (const request of http.match(() => true)) {
        seen.push(request);
        if (request.request.url.includes('/calendar')) {
          request.flush(bodies.calendar ?? emptyCalendar);
        } else if (request.request.url.includes('/timeline')) {
          request.flush(bodies.timeline ?? emptyPage);
        } else {
          request.flush(bodies.other ?? {});
        }
      }
    }
    await operation;
    return seen;
  }

  it('abre o grupo carregando a linha do tempo e o calendário, sem repetir chamadas', async () => {
    const seen = await settle(store.open('g1'), {
      timeline: { items: [mediaEntry('m1', '2026-10-13T18:00:00Z')], nextCursor: null, hasMore: false },
      calendar: { months: [{ month: '2026-10', count: 1, days: [{ date: '2026-10-13', count: 1 }] }], total: 1 },
    });

    expect(store.entries()).toHaveLength(1);
    expect(store.hasMore()).toBe(false);
    expect(store.calendar()?.total).toBe(1);
    // Uma chamada de timeline e uma de calendário: o calendário não é pedido duas vezes.
    expect(seen.filter((r) => r.request.url.includes('/calendar'))).toHaveLength(1);
  });

  it('a página seguinte usa o cursor e acumula as entradas', async () => {
    await settle(store.open('g1'), {
      timeline: { items: [mediaEntry('m1', '2026-10-13T18:00:00Z')], nextCursor: 'cursor-1', hasMore: true },
    });
    expect(store.hasMore()).toBe(true);

    const seen = await settle(store.loadNext(), {
      timeline: { items: [mediaEntry('m2', '2026-10-13T19:00:00Z')], nextCursor: null, hasMore: false },
    });

    const timelineRequest = seen.find((r) => r.request.url.includes('/timeline'))!;
    expect(timelineRequest.request.params.get('cursor')).toBe('cursor-1');
    expect(store.entries().map((e) => e.media?.media.id)).toEqual(['m1', 'm2']);
    expect(store.hasMore()).toBe(false);
  });

  it('filtrar por dia troca para a rota do dia e envia o fuso do navegador', async () => {
    await settle(store.open('g1'));

    const seen = await settle(store.selectDay('2026-10-13'), {
      timeline: { items: [mediaEntry('m1', '2026-10-13T18:00:00Z')], nextCursor: null, hasMore: false },
    });

    const dayRequest = seen.find((r) => r.request.url.includes('/timeline/day/2026-10-13'))!;
    expect(dayRequest).toBeDefined();
    expect(dayRequest.request.params.get('timeZone')).toBeTruthy();
    expect(store.selectedDay()).toBe('2026-10-13');
    expect(store.entries()).toHaveLength(1);
  });

  it('criar um momento recarrega a linha do tempo', async () => {
    await settle(store.open('g1'));

    const creating = store.createMoment({
      title: 'Corujão da LetalCompany',
      startsAt: '2026-10-13T22:00:00Z',
      endsAt: '2026-10-14T06:00:00Z',
    });
    const seen = await settle(creating, {
      other: { id: 'mo1', title: 'Corujão da LetalCompany' },
      timeline: {
        items: [
          {
            kind: 'MOMENT',
            at: '2026-10-13T22:00:00Z',
            moment: {
              moment: { id: 'mo1', title: 'Corujão da LetalCompany', mediaCount: 0 },
              media: [],
              commentCount: 0,
              commentFeed: [],
            },
            media: null,
          },
        ],
        nextCursor: null,
        hasMore: false,
      },
    });

    expect(seen.some((r) => r.request.url === '/api/groups/g1/moments')).toBe(true);
    expect(store.entries()[0].moment?.moment.title).toBe('Corujão da LetalCompany');
  });

  it('sem grupo aberto, carregar a próxima página não chama a API', async () => {
    await store.loadNext();
    expect(store.entries()).toHaveLength(0);
  });
});
