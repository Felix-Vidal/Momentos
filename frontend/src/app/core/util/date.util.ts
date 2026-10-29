/** Formatação de datas em português do Brasil. */

const DIA_MES = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'long' });
const DIA_MES_ANO = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: 'long',
  year: 'numeric',
});
const HORA = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });
const MES_ANO = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' });

export function formatDay(iso: string): string {
  const date = new Date(iso);
  const sameYear = date.getFullYear() === new Date().getFullYear();
  return (sameYear ? DIA_MES : DIA_MES_ANO).format(date);
}

export function formatTime(iso: string): string {
  return HORA.format(new Date(iso));
}

export function formatDayAndTime(iso: string): string {
  return `${formatDay(iso)} às ${formatTime(iso)}`;
}

export function formatMonth(month: string): string {
  // `month` chega como "2026-10"; o dia 15 evita surpresas de fuso na virada do mês.
  return MES_ANO.format(new Date(`${month}-15T12:00:00`));
}

/**
 * Intervalo de um Momento em uma linha curta:
 * mesmo dia → "13 de outubro, 22:00–06:00"; dias diferentes → "13 de out, 22:00 → 14 de out, 06:00".
 */
export function formatInterval(startsAt: string, endsAt: string): string {
  const start = new Date(startsAt);
  const end = new Date(endsAt);
  const sameDay = start.toDateString() === end.toDateString();

  if (sameDay) {
    return `${formatDay(startsAt)}, ${formatTime(startsAt)}–${formatTime(endsAt)}`;
  }
  return `${formatDay(startsAt)}, ${formatTime(startsAt)} → ${formatDay(endsAt)}, ${formatTime(endsAt)}`;
}

/** Duração legível de um vídeo, a partir dos segundos devolvidos pela API. */
export function formatDuration(seconds: string | number | null): string | null {
  if (seconds === null) {
    return null;
  }
  const total = Math.round(Number(seconds));
  if (!Number.isFinite(total) || total <= 0) {
    return null;
  }
  const minutes = Math.floor(total / 60);
  const rest = total % 60;
  return `${minutes}:${String(rest).padStart(2, '0')}`;
}

/**
 * Dia local de um instante, como "2026-10-13". É a chave usada para juntar as
 * mídias soltas da linha do tempo sob um único cabeçalho de data.
 */
export function dayKey(iso: string): string {
  const date = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** Fuso do navegador, enviado à API para interpretar EXIF e agrupar por dia. */
export function browserTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone;
}

/** Converte o valor de um <input type="datetime-local"> em ISO com fuso. */
export function localInputToIso(value: string): string {
  return new Date(value).toISOString();
}

/** Converte ISO em valor para <input type="datetime-local">, no fuso local. */
export function isoToLocalInput(iso: string): string {
  const date = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
