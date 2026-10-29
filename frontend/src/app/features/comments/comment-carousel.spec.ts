import { ComponentRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CommentCarousel } from './comment-carousel';
import { CommentView } from '../../core/api/timeline.models';

function comment(id: string, name: string, body: string): CommentView {
  return {
    id,
    groupId: 'g1',
    mediaId: 'm1',
    momentId: null,
    body,
    author: { id: 'u-' + id, name, avatarMediaId: null },
    createdAt: '2026-10-13T23:00:00Z',
    updatedAt: '2026-10-13T23:00:00Z',
    edited: false,
    canEdit: false,
    canDelete: false,
    mediaThumbnail: 'm1',
  };
}

describe('CommentCarousel', () => {
  let ref: ComponentRef<CommentCarousel>;

  beforeEach(() => {
    vi.useFakeTimers();
    // jsdom não implementa matchMedia; a caixa consulta prefers-reduced-motion.
    window.matchMedia = ((query: string) => ({
      matches: false,
      media: query,
      addEventListener: () => {},
      removeEventListener: () => {},
    })) as unknown as typeof window.matchMedia;

    TestBed.configureTestingModule({ imports: [CommentCarousel] });
  });

  afterEach(() => vi.useRealTimers());

  function mount(comments: CommentView[], visible = true) {
    const fixture = TestBed.createComponent(CommentCarousel);
    ref = fixture.componentRef;
    ref.setInput('comments', comments);
    ref.setInput('visible', visible);
    fixture.detectChanges();
    return fixture;
  }

  it('sem comentários, convida a ser o primeiro', () => {
    const fixture = mount([]);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Seja o primeiro a comentar',
    );
  });

  it('mostra autor e texto do comentário atual', () => {
    const fixture = mount([comment('c1', 'Ana', 'Essa rodada decidiu tudo')]);
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(html).toContain('Ana');
    expect(html).toContain('Essa rodada decidiu tudo');
  });

  it('com um único comentário, não fica trocando', () => {
    const fixture = mount([comment('c1', 'Ana', 'Único')]);

    vi.advanceTimersByTime(20000);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Único');
  });

  it('troca de comentário em loop, voltando ao primeiro', () => {
    const fixture = mount([
      comment('c1', 'Ana', 'Primeiro'),
      comment('c2', 'Bruno', 'Segundo'),
    ]);
    const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text()).toContain('Primeiro');

    // A duração é 3,5 s mais uma folga por caractere: "Primeiro" sai perto de 3,7 s.
    vi.advanceTimersByTime(3000);
    fixture.detectChanges();
    expect(text()).toContain('Primeiro');

    vi.advanceTimersByTime(1000);
    fixture.detectChanges();
    expect(text()).toContain('Segundo');

    vi.advanceTimersByTime(4000);
    fixture.detectChanges();
    expect(text()).toContain('Primeiro');
  });

  it('comentário longo é truncado com reticências', () => {
    const longo = 'a'.repeat(200);
    const fixture = mount([comment('c1', 'Ana', longo)]);

    const paragraph = (fixture.nativeElement as HTMLElement).querySelector('p')!;
    expect(paragraph.textContent!.endsWith('…')).toBe(true);
    expect(paragraph.textContent!.length).toBeLessThan(longo.length);
  });

  it('comentário longo permanece mais tempo que um curto', () => {
    const fixture = mount([
      comment('c1', 'Ana', 'a'.repeat(120)),
      comment('c2', 'Bruno', 'Curto'),
    ]);
    const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

    // Com 120 caracteres a duração passa de 6s (limitada ao máximo).
    vi.advanceTimersByTime(4000);
    fixture.detectChanges();
    expect(text()).not.toContain('Curto');

    vi.advanceTimersByTime(2500);
    fixture.detectChanges();
    expect(text()).toContain('Curto');
  });

  it('escondida, a caixa não gira e volta ao primeiro comentário', () => {
    const fixture = mount(
      [comment('c1', 'Ana', 'Primeiro'), comment('c2', 'Bruno', 'Segundo')],
      true,
    );
    vi.advanceTimersByTime(4000);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Segundo');

    ref.setInput('visible', false);
    fixture.detectChanges();
    vi.advanceTimersByTime(20000);
    fixture.detectChanges();

    // Ao reaparecer, começa do início.
    ref.setInput('visible', true);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Primeiro');
  });

  it('no feed de um momento, mostra a miniatura da mídia comentada', () => {
    const fixture = mount([comment('c1', 'Ana', 'Comentário com foto')]);
    ref.setInput('showMediaThumbnail', true);
    fixture.detectChanges();

    const img = (fixture.nativeElement as HTMLElement).querySelector('img')!;
    expect(img.getAttribute('src')).toBe('/api/media/m1/thumbnail');
  });

  it('o indicador de progresso tem um ponto por comentário', () => {
    const fixture = mount([
      comment('c1', 'Ana', 'Um'),
      comment('c2', 'Bruno', 'Dois'),
      comment('c3', 'Caio', 'Três'),
    ]);

    const dots = (fixture.nativeElement as HTMLElement).querySelectorAll('[aria-hidden] > span');
    expect(dots.length).toBe(3);
  });
});
