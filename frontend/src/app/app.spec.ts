import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes)],
    }).compileComponents();
  });

  it('monta o shell da aplicação', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('mostra a marca e os links principais de navegação', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;

    expect(html.textContent).toContain('Momentos');
    expect(html.querySelector('a[href="/grupos"]')?.textContent).toContain('Meus grupos');
    expect(html.querySelector('a[href="/descobrir"]')?.textContent).toContain('Descobrir');
  });
});
