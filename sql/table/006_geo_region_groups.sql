-- TABLE
create table if not exists public.geo_region_groups (
  id uuid primary key default gen_random_uuid(), -- description: Internal ID (UUID)

  adm_id text not null unique,                   -- description: Source/admin id for this group (ADM1-level identifier)
  adm_name text not null,                        -- description: Display name for this group (default language)

  created_at timestamptz not null default now(), -- description: Row creation time
  updated_at timestamptz not null default now()  -- description: Row last update time
);

comment on table public.geo_region_groups is 'Geo region group (ADM1-level like prefecture/state).';
comment on column public.geo_region_groups.id is 'Internal primary key (UUID).';
comment on column public.geo_region_groups.adm_id is 'ADM1-level identifier used by the app/data source.';
comment on column public.geo_region_groups.adm_name is 'Group display name.';
comment on column public.geo_region_groups.created_at is 'Creation timestamp.';
comment on column public.geo_region_groups.updated_at is 'Last updated timestamp.';

-- RLS
