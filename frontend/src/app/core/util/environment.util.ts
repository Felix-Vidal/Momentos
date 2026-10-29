/**
 * Consultas ao ambiente que não podem derrubar a interface.
 *
 * <p>`matchMedia` não existe em todo lugar (jsdom nos testes, renderização no
 * servidor, navegadores antigos). Como essas respostas só ajustam animação e
 * interação, qualquer falha vira o padrão mais seguro em vez de uma exceção que
 * impediria o componente de renderizar.
 */
function query(view: Window | null | undefined, media: string): boolean | null {
  try {
    if (!view || typeof view.matchMedia !== 'function') {
      return null;
    }
    return view.matchMedia(media).matches;
  } catch {
    return null;
  }
}

/** Verdadeiro quando a pessoa pediu menos movimento. Na dúvida, anima. */
export function prefersReducedMotion(view?: Window | null): boolean {
  return query(view ?? globalThis.window, '(prefers-reduced-motion: reduce)') ?? false;
}

/** Verdadeiro quando o dispositivo tem hover de verdade. Na dúvida, assume que sim. */
export function hasHover(view?: Window | null): boolean {
  return query(view ?? globalThis.window, '(hover: hover)') ?? true;
}
