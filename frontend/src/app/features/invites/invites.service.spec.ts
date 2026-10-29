import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { InvitesService } from './invites.service';

describe('InvitesService', () => {
  let service: InvitesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InvitesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('o link de convite aponta para a rota /convite/:token da própria origem', () => {
    expect(service.linkFor('abc123')).toBe(`${location.origin}/convite/abc123`);
  });

  it('criar convite dirigido envia o e-mail e o papel', async () => {
    const done = service.create('g1', { email: 'ana@ufac.br', role: 'MEMBER', maxUses: 1 });
    const req = http.expectOne('/api/groups/g1/invites');

    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'ana@ufac.br', role: 'MEMBER', maxUses: 1 });
    req.flush({ id: 'i1', token: 'tok', invitedEmail: 'ana@ufac.br' });

    expect((await done).token).toBe('tok');
  });

  it('a prévia do convite não exige estar logado e devolve o motivo quando inválido', async () => {
    const done = service.preview('expirado');
    http.expectOne('/api/invites/expirado').flush({
      groupId: null,
      groupName: null,
      valid: false,
      alreadyMember: false,
      reason: 'Este convite expirou.',
    });

    const preview = await done;
    expect(preview.valid).toBe(false);
    expect(preview.reason).toBe('Este convite expirou.');
  });

  it('aceitar o convite devolve o grupo em que a pessoa entrou', async () => {
    const done = service.accept('tok');
    http.expectOne('/api/invites/tok/accept').flush({ groupId: 'g1' });
    expect((await done).groupId).toBe('g1');
  });
});
