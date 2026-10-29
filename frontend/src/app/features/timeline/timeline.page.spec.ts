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

const page = {
  items: [
    {
      kind: 'MEDIA' as const,
      at: '2026-10-13T18:00:00Z',
      moment: null,
      media: {
        media: {
          id: 'm1',
          type: 'IMAGE',
          caption: 'A roleta decidiu',
          capturedAt: '2026-10-13T18:00:00Z',
          width: 1400,
          height: 1000,
          hasThumbnail: true,
          owner: { id: 'u1', name: 'Félix', avatarMediaId: null },
          durationSeconds: null,
        },
        commentCount: 0,
        comments: [],
      },
    },
  ],
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

  async function settle(): Promise<number> {
    let timelineRequests = 0;
    for (let tick = 0; tick < 12; tick++) {
      await new Promise((resolve) => setTimeout(resolve, 0));
      for (const request of http.match(() => true)) {
        if (request.request.url.includes('/calendar')) {
          request.flush({ months: [], total: 0 });
        } else {
          timelineRequests++;
          request.flush(page);
        }
      }
    }
    return timelineRequests;
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
