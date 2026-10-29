import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EMPTY } from 'rxjs';
import { StompService } from '../../core/realtime/stomp.service';
import { GroupShellPage } from '../groups/group-shell.page';
import { TimelinePage } from './timeline.page';

const group = {
  id: 'g1',
  slug: 'sociedade-da-roleta',
  name: 'Sociedade da Roleta',
  myRole: 'ADMIN' as const,
};

function media(id: string, at: string) {
  return {
    media: {
      id,
      type: 'IMAGE',
      caption: 'Foto ' + id,
      capturedAt: at,
      width: 1400,
      height: 1000,
      hasThumbnail: true,
      owner: { id: 'u1', name: 'Félix', avatarMediaId: null },
      durationSeconds: null,
    },
    commentCount: 0,
    comments: [],
  };
}

function loose(id: string, at: string) {
  return { kind: 'MEDIA' as const, at, moment: null, media: media(id, at) };
}

function momentEntry(id: string, title: string, at: string, mediaIds: string[]) {
  return {
    kind: 'MOMENT' as const,
    at,
    media: null,
    moment: {
      moment: {
        id,
        title,
        description: null,
        startsAt: at,
        endsAt: at,
        mediaCount: mediaIds.length,
        createdBy: { id: 'u1', name: 'Félix', avatarMediaId: null },
        createdAt: at,
        groupId: 'g1',
      },
      media: mediaIds.map((mediaId) => media(mediaId, at)),
      commentCount: 0,
      commentFeed: [],
    },
  };
}

const page = {
  items: [loose('m1', '2026-10-13T18:00:00')],
  nextCursor: null,
  hasMore: false,
};

describe('TimelinePage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TimelinePage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        // A página roda dentro da moldura do grupo; aqui ela é substituída por um duplo.
        {
          provide: GroupShellPage,
          useValue: { group: { value: signal(group) }, groupId: signal('g1') },
        },
        // Sem WebSocket no teste.
        { provide: StompService, useValue: { comments: () => EMPTY } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function settle(body: object = page): Promise<number> {
    let timelineRequests = 0;
    for (let tick = 0; tick < 12; tick++) {
      await new Promise((resolve) => setTimeout(resolve, 0));
      for (const request of http.match(() => true)) {
        if (request.request.url.includes('/calendar')) {
          request.flush({ months: [], total: 0 });
        } else {
          timelineRequests++;
          request.flush(body);
        }
      }
    }
    return timelineRequests;
  }

  /** Monta a página com a resposta informada e devolve o HTML já renderizado. */
  async function render(body: object): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(TimelinePage);
    fixture.detectChanges();
    await settle(body);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  /** Cabeçalhos de data das mídias soltas, na ordem em que aparecem. */
  function dayHeadings(html: HTMLElement): string[] {
    return [...html.querySelectorAll('li > div > header > p')].map(
      (node) => node.textContent?.trim() ?? '',
    );
  }

  it('carrega a linha do tempo uma única vez ao abrir o grupo', async () => {
    const fixture = TestBed.createComponent(TimelinePage);
    fixture.detectChanges();

    const requests = await settle();

    // Regressão: o carregamento era disparado de dentro de um effect que acabava
    // rastreando os sinais escritos pelo próprio store, entrando em laço e deixando
    // a tela presa no esqueleto.
    expect(requests).toBe(1);
  });

  it('mostra as mídias recebidas em vez do esqueleto', async () => {
    const fixture = TestBed.createComponent(TimelinePage);
    fixture.detectChanges();
    await settle();
    await fixture.whenStable();
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    const image = html.querySelector('figure img') as HTMLImageElement | null;

    expect(image).not.toBeNull();
    // Angular aplica `src` como propriedade; o jsdom a resolve para URL absoluta.
    expect(image!.src).toContain('/api/media/m1/thumbnail');
    expect(html.textContent).toContain('13 de outubro');
    // O esqueleto some assim que a primeira página chega.
    expect(html.querySelector('.animate-pulse')).toBeNull();
  });
});

describe('TimelinePage: agrupamento das mídias soltas', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TimelinePage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: GroupShellPage,
          useValue: { group: { value: signal(group) }, groupId: signal('g1') },
        },
        { provide: StompService, useValue: { comments: () => EMPTY } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function settle(body: object): Promise<void> {
    for (let tick = 0; tick < 12; tick++) {
      await new Promise((resolve) => setTimeout(resolve, 0));
      for (const request of http.match(() => true)) {
        request.flush(
          request.request.url.includes('/calendar') ? { months: [], total: 0 } : body,
        );
      }
    }
  }

  async function render(body: object): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(TimelinePage);
    fixture.detectChanges();
    await settle(body);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  function dayHeadings(html: HTMLElement): string[] {
    return [...html.querySelectorAll('li > div > header > p')].map(
      (node) => node.textContent?.trim() ?? '',
    );
  }

  it('duas fotos do mesmo dia ficam sob um único cabeçalho de data', async () => {
    const html = await render({
      items: [loose('m1', '2026-10-13T12:10:00'), loose('m2', '2026-10-13T12:42:00')],
      nextCursor: null,
      hasMore: false,
    });

    expect(dayHeadings(html)).toEqual(['13 de outubro']);
    expect(html.querySelectorAll('figure').length).toBe(2);
    expect(html.textContent).toContain('2 mídias');
  });

  it('dias diferentes continuam em blocos separados', async () => {
    const html = await render({
      items: [loose('m1', '2026-10-13T12:10:00'), loose('m2', '2026-10-14T09:00:00')],
      nextCursor: null,
      hasMore: false,
    });

    expect(dayHeadings(html)).toEqual(['13 de outubro', '14 de outubro']);
  });

  it('um Momento no meio do dia separa o que veio antes do que veio depois', async () => {
    const html = await render({
      items: [
        loose('m1', '2026-10-13T09:00:00'),
        momentEntry('mo1', 'Almoço no RU', '2026-10-13T11:30:00', ['m2']),
        loose('m3', '2026-10-13T18:00:00'),
      ],
      nextCursor: null,
      hasMore: false,
    });

    // A cronologia vem antes da economia de cabeçalhos: a data se repete, mas
    // nenhuma foto é movida para fora do seu lugar na linha do tempo.
    expect(dayHeadings(html)).toEqual(['13 de outubro', '13 de outubro']);
    expect(html.textContent).toContain('Almoço no RU');
  });

  it('mídia solta de um dia só mostra "1 mídia" no singular', async () => {
    const html = await render({
      items: [loose('m1', '2026-10-13T12:10:00')],
      nextCursor: null,
      hasMore: false,
    });

    expect(html.textContent).toContain('1 mídia');
    expect(html.textContent).not.toContain('1 mídias');
  });
});
