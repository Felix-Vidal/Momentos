-- Mídias pertencem a quem enviou; aparecem em um grupo por compartilhamento explícito.
create table media (
    id                 uuid         primary key default gen_random_uuid(),
    owner_id           uuid         not null references users (id) on delete cascade,
    type               varchar(16)  not null,
    storage_key        varchar(255) not null,
    thumbnail_key      varchar(255),
    mime_type          varchar(100) not null,
    size_bytes         bigint       not null,
    width              int,
    height             int,
    duration_seconds   numeric(10, 3),
    captured_at        timestamptz  not null,
    captured_at_source varchar(16)  not null,
    caption            varchar(280),
    uploaded_at        timestamptz  not null default now(),
    constraint media_type_check check (type in ('IMAGE', 'VIDEO')),
    constraint media_captured_source_check
        check (captured_at_source in ('EXIF', 'UPLOAD', 'MANUAL'))
);

create index media_owner_idx on media (owner_id, captured_at desc);

create table media_group_shares (
    id         uuid        primary key default gen_random_uuid(),
    media_id   uuid        not null references media (id) on delete cascade,
    group_id   uuid        not null references groups (id) on delete cascade,
    shared_by  uuid        not null references users (id),
    shared_at  timestamptz not null default now(),
    constraint media_group_shares_unique unique (media_id, group_id)
);

-- A linha do tempo percorre as mídias de um grupo em ordem cronológica.
create index media_group_shares_timeline_idx on media_group_shares (group_id, media_id);

-- Agora que `media` existe, a foto de perfil e a capa do grupo podem apontar para ela.
alter table users
    add constraint users_avatar_fk foreign key (avatar_media_id)
        references media (id) on delete set null;

alter table groups
    add constraint groups_cover_fk foreign key (cover_media_id)
        references media (id) on delete set null;
