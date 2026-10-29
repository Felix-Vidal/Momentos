-- Baseline do schema: extensões de que as demais migrações dependem.
-- `unaccent` serve à busca pública de grupos abertos por nome, ignorando acentos.
create extension if not exists unaccent;

-- Função imutável para indexar nome de grupo sem acento (unaccent não é imutável por padrão).
create or replace function momentos_slugify_search(texto text)
    returns text
    language sql
    immutable
    strict
    parallel safe
as $$
    select lower(public.unaccent('public.unaccent', texto));
$$;
