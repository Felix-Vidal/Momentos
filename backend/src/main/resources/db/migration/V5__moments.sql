-- Momentos: um intervalo de data e hora com título, que agrupa as mídias do período.
-- O intervalo é meio-aberto [starts_at, ends_at): uma mídia pertence ao Momento
-- quando sua captura cai dentro dele.
create table moments (
    id          uuid         primary key default gen_random_uuid(),
    group_id    uuid         not null references groups (id) on delete cascade,
    title       varchar(120) not null,
    description varchar(500),
    starts_at   timestamptz  not null,
    ends_at     timestamptz  not null,
    created_by  uuid         not null references users (id),
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),
    constraint moments_interval_check check (ends_at > starts_at)
);

create index moments_group_start_idx on moments (group_id, starts_at);
