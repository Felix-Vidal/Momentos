-- Comentários existem sempre no contexto de um grupo e apontam para uma mídia
-- OU para um Momento (nunca os dois, nunca nenhum).
--
-- O group_id obrigatório é o que impede o vazamento entre grupos: a mesma foto
-- compartilhada em dois grupos tem conversas separadas em cada um.
create table comments (
    id         uuid        primary key default gen_random_uuid(),
    group_id   uuid        not null references groups (id) on delete cascade,
    author_id  uuid        not null references users (id) on delete cascade,
    body       varchar(1000) not null,
    media_id   uuid        references media (id) on delete cascade,
    moment_id  uuid        references moments (id) on delete cascade,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint comments_target_check
        check ((media_id is not null) <> (moment_id is not null))
);

create index comments_media_idx on comments (media_id, group_id, created_at);
create index comments_moment_idx on comments (moment_id, created_at);
create index comments_group_idx on comments (group_id, created_at desc);
