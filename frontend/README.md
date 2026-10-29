# Momentos — frontend

Angular 22 (standalone components, signals, zoneless) + Tailwind CSS 4.

```bash
npm install
npm start     # http://localhost:4200, com proxy de /api e /ws para http://localhost:8080
npm test      # Vitest
npm run build
```

Organização de `src/app`:

- `core/` — infraestrutura: autenticação, interceptors HTTP, STOMP, utilitários de data/mídia
- `shared/` — UI reutilizável (`ui/`) e diretivas (`directives/`)
- `features/` — telas por funcionalidade: `auth`, `groups`, `timeline`, `media`, `comments`,
  `members`, `invites`

O tema visual (cores, fontes, raios, sombras e o utilitário `vidro` do efeito glassmorphism)
fica em `src/styles.css`, no bloco `@theme` do Tailwind 4 — não há `tailwind.config.js`.

Documentação geral do projeto: [`../README.md`](../README.md).
