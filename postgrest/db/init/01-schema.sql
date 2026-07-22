-- Schema exposed to PostgREST.
-- Kept separate from other internal schemas so we never expose
-- more than intended (PostgREST only sees what PGRST_DB_SCHEMAS lists).
create schema if not exists api;

create table if not exists api.todos (
    id          bigint generated always as identity primary key,
    task        text not null,
    is_done     boolean not null default false,
    inserted_at timestamptz not null default now()
);

-- RLS is on by default; only the policy below grants read access.
-- Nothing is writable until you add explicit policies for that.
alter table api.todos enable row level security;

create policy todos_select_all on api.todos
    for select
    using (true);

insert into api.todos (task, is_done)
values ('Learn PostgREST', false),
       ('Deploy with Docker Compose', true)
on conflict do nothing;
