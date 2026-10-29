-- Categorias são do grupo: cada turma inventa as suas (festa, almoço, tereré,
-- corujão, jogos…). Como a mesma foto pode estar em vários grupos, a marcação
-- vive no contexto de um grupo — a categoria é que carrega o group_id.
create table categories (
    id         uuid        primary key default gen_random_uuid(),
    group_id   uuid        not null references groups (id) on delete cascade,
    name       varchar(40) not null,
    color      varchar(7)  not null,
    emoji      varchar(16),
    created_by uuid        not null references users (id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint categories_color_check check (color ~ '^#[0-9a-fA-F]{6}$')
);

-- Dois nomes iguais no mesmo grupo seriam indistinguíveis na tela; a comparação
-- ignora acento e caixa, como a busca de grupos.
create unique index categories_group_name_key
    on categories (group_id, momentos_slugify_search(name));

create table media_categories (
    id          uuid        primary key default gen_random_uuid(),
    media_id    uuid        not null references media (id) on delete cascade,
    category_id uuid        not null references categories (id) on delete cascade,
    tagged_by   uuid        not null references users (id),
    tagged_at   timestamptz not null default now(),
    constraint media_categories_unique unique (media_id, category_id)
);

create index media_categories_category_idx on media_categories (category_id);
