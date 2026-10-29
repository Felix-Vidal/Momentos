import { formatDuration, formatInterval, isoToLocalInput, localInputToIso } from './date.util';

describe('date.util', () => {
  it('intervalo no mesmo dia mostra as duas horas juntas', () => {
    const texto = formatInterval('2026-10-13T15:00:00', '2026-10-13T18:00:00');
    expect(texto).toContain('13 de outubro');
    expect(texto).toContain('–');
  });

  it('intervalo que atravessa a meia-noite mostra os dois dias', () => {
    // O caso do Corujão: 13/10 22:00 → 14/10 06:00.
    const texto = formatInterval('2026-10-13T22:00:00', '2026-10-14T06:00:00');
    expect(texto).toContain('13 de outubro');
    expect(texto).toContain('14 de outubro');
    expect(texto).toContain('→');
  });

  it('duração de vídeo vira minutos e segundos', () => {
    expect(formatDuration('95.4')).toBe('1:35');
    expect(formatDuration(8)).toBe('0:08');
    expect(formatDuration(null)).toBeNull();
    expect(formatDuration(0)).toBeNull();
  });

  it('a conversão para o input de data e volta preserva o instante', () => {
    const iso = new Date('2026-10-13T22:30:00').toISOString();
    expect(localInputToIso(isoToLocalInput(iso))).toBe(iso);
  });
});
