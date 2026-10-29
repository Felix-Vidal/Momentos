import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GroupsService } from './groups.service';

const group = {
  id: 'g1',
  slug: 'sociedade-da-roleta',
  name: 'Sociedade da Roleta',
  description: null,
  coverMediaId: null,
  visibility: 'OPEN' as const,
  theme: 'ROULETTE' as const,
  myRole: 'ADMIN' as const,
  memberCount: 1,
  createdAt: '2026-10-29T12:00:00Z',
};

describe('GroupsService', () => {
  let service: GroupsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GroupsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('criar um grupo avança a revisão, para as listas recarregarem', async () => {
    const before = service.revision();

    const done = service.create({ name: 'Sociedade da Roleta', visibility: 'OPEN' });
    http.expectOne('/api/groups').flush(group);
    await done;

    expect(service.revision()).toBe(before + 1);
  });

  it('a busca pública envia o termo e a página', async () => {
    const done = service.discover('roleta');
    const req = http.expectOne((r) => r.url === '/api/groups/public');

    expect(req.request.params.get('q')).toBe('roleta');
    expect(req.request.params.get('page')).toBe('0');
    req.flush({ content: [], page: 0, size: 12, totalElements: 0, totalPages: 0, last: true });
    await done;
  });

  it('remover membro também avança a revisão', async () => {
    const before = service.revision();

    const done = service.removeMember('g1', 'u2');
    http.expectOne('/api/groups/g1/members/u2').flush(null);
    await done;

    expect(service.revision()).toBe(before + 1);
  });

  it('abrir grupo pelo slug usa a rota by-slug', async () => {
    const done = service.bySlug('sociedade-da-roleta');
    http.expectOne('/api/groups/by-slug/sociedade-da-roleta').flush(group);
    expect((await done).name).toBe('Sociedade da Roleta');
  });
});
