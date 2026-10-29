import { Directive, ElementRef, inject, OnDestroy, OnInit, output } from '@angular/core';
import { prefersReducedMotion } from '../../core/util/environment.util';

/**
 * Anima a entrada do elemento quando ele aparece na tela e avisa quando chega perto
 * do fim — usado pela linha do tempo para carregar a página seguinte.
 *
 * <p>Quem prefere menos movimento recebe o conteúdo já visível, sem animação.
 */
@Directive({
  selector: '[appInView]',
})
export class InViewDirective implements OnInit, OnDestroy {
  /** Emitido na primeira vez que o elemento entra na área visível. */
  readonly entered = output<void>();

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private observer?: IntersectionObserver;

  ngOnInit(): void {
    const element = this.host.nativeElement;
    const reduced = prefersReducedMotion(element.ownerDocument.defaultView);

    if (reduced || typeof IntersectionObserver === 'undefined') {
      element.dataset['inView'] = 'true';
      this.entered.emit();
      return;
    }

    element.dataset['inView'] = 'false';
    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            element.dataset['inView'] = 'true';
            this.entered.emit();
            this.observer?.disconnect();
          }
        }
      },
      { rootMargin: '0px 0px -10% 0px', threshold: 0.05 },
    );
    this.observer.observe(element);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
