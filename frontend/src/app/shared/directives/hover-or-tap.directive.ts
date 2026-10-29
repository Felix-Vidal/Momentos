import { Directive, HostListener, inject, OnDestroy, output, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';

/**
 * Mesma intenção em dois mundos de entrada.
 *
 * <p>No desktop, passar o mouse (ou focar pelo teclado) ativa; sair desativa.
 * No celular, onde não existe hover, o primeiro toque ativa e o segundo confirma —
 * é assim que a caixa de comentários aparece com um toque e o lightbox abre com o
 * seguinte.
 */
@Directive({
  selector: '[appHoverOrTap]',
  exportAs: 'hoverOrTap',
})
export class HoverOrTapDirective implements OnDestroy {
  /** Ativado: mostrar a caixa de comentários. */
  readonly activated = output<void>();
  /** Desativado: esconder a caixa. */
  readonly deactivated = output<void>();
  /** Confirmado: abrir o lightbox. */
  readonly confirmed = output<void>();

  private readonly document = inject(DOCUMENT);
  private readonly active = signal(false);
  private touchActive = false;
  private closeOutside?: (event: Event) => void;

  readonly isActive = this.active.asReadonly();

  private get hasHover(): boolean {
    return this.document.defaultView?.matchMedia('(hover: hover)').matches ?? true;
  }

  @HostListener('mouseenter')
  onMouseEnter(): void {
    if (this.hasHover) {
      this.activate();
    }
  }

  @HostListener('mouseleave')
  onMouseLeave(): void {
    if (this.hasHover) {
      this.deactivate();
    }
  }

  @HostListener('focusin')
  onFocusIn(): void {
    this.activate();
  }

  @HostListener('focusout')
  onFocusOut(): void {
    if (this.hasHover) {
      this.deactivate();
    }
  }

  /** No toque, o clique só confirma se a caixa já estava aberta. */
  @HostListener('click', ['$event'])
  onClick(event: MouseEvent): void {
    if (this.hasHover) {
      this.confirmed.emit();
      return;
    }
    if (this.touchActive) {
      this.confirmed.emit();
      return;
    }
    // Primeiro toque: só revela a caixa.
    event.preventDefault();
    event.stopPropagation();
    this.activate();
    this.touchActive = true;
    this.listenForOutsideTap();
  }

  @HostListener('keydown.escape')
  onEscape(): void {
    this.deactivate();
  }

  ngOnDestroy(): void {
    this.removeOutsideListener();
  }

  private activate(): void {
    if (!this.active()) {
      this.active.set(true);
      this.activated.emit();
    }
  }

  private deactivate(): void {
    if (this.active()) {
      this.active.set(false);
      this.touchActive = false;
      this.deactivated.emit();
    }
    this.removeOutsideListener();
  }

  /** Tocar em outro lugar fecha a caixa, como o mouse saindo faria. */
  private listenForOutsideTap(): void {
    this.removeOutsideListener();
    this.closeOutside = () => this.deactivate();
    setTimeout(() => {
      if (this.closeOutside) {
        this.document.addEventListener('click', this.closeOutside, { once: true });
      }
    });
  }

  private removeOutsideListener(): void {
    if (this.closeOutside) {
      this.document.removeEventListener('click', this.closeOutside);
      this.closeOutside = undefined;
    }
  }
}
