import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MediaService, UploadProgress } from './media.service';

describe('MediaService', () => {
  let service: MediaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MediaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia o arquivo como multipart, com o grupo e o fuso do navegador', () => {
    const file = new File(['conteudo'], 'corujao.jpg', { type: 'image/jpeg' });

    service.uploadOne(file, ['g1']).subscribe();
    const req = http.expectOne((r) => r.url.startsWith('/api/media'));

    expect(req.request.method).toBe('POST');
    expect(req.request.url).toContain('groupIds=g1');
    expect(req.request.url).toContain('timeZone=');
    expect(req.request.body instanceof FormData).toBe(true);
    expect((req.request.body as FormData).get('files')).toBeInstanceOf(File);

    req.flush({ uploaded: [], rejected: [] });
  });

  it('relata progresso e conclui com o resultado do servidor', () => {
    const file = new File(['x'], 'foto.jpg', { type: 'image/jpeg' });
    const events: UploadProgress[] = [];

    service.uploadOne(file, ['g1']).subscribe((progress) => events.push(progress));
    const req = http.expectOne((r) => r.url.startsWith('/api/media'));

    req.event({ type: 1, loaded: 50, total: 100 } as never);
    req.flush({ uploaded: [{ id: 'm1' }], rejected: [] });

    expect(events[0].percent).toBe(50);
    expect(events[0].done).toBe(false);
    expect(events.at(-1)!.done).toBe(true);
    expect(events.at(-1)!.result?.uploaded).toHaveLength(1);
  });

  it('um envio concluído avança a revisão, para a linha do tempo recarregar', () => {
    const before = service.revision();
    const file = new File(['x'], 'foto.jpg', { type: 'image/jpeg' });

    service.uploadOne(file, ['g1']).subscribe();
    http.expectOne((r) => r.url.startsWith('/api/media')).flush({ uploaded: [], rejected: [] });

    expect(service.revision()).toBe(before + 1);
  });

  it('corrigir a data de captura usa PATCH na mídia', async () => {
    const done = service.update('m1', { capturedAt: '2026-10-13T22:30:00Z' });
    const req = http.expectOne('/api/media/m1');

    expect(req.request.method).toBe('PATCH');
    req.flush({ id: 'm1', capturedAtSource: 'MANUAL' });
    expect((await done).capturedAtSource).toBe('MANUAL');
  });
});
