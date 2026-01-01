-- TABLE
create table if not exists public.geo_regions (
  id uuid primary key default gen_random_uuid(), -- description: Internal ID (UUID)

  group_id uuid not null references public.geo_region_groups(id) on delete cascade, -- description: Parent group (FK)
  adm2_id text not null,                          -- description: ADM2-level identifier (e.g. municipality id)
  name text not null,                             -- description: Default display name
  name_en text,                                   -- description: English name (nullable)
  name_ja text,                                   -- description: Japanese name (nullable)
  wikipedia text,                                 -- description: Wikipedia tag/value (nullable)
  iso3166_2 text,                                 -- description: ISO 3166-2 code (nullable)
  thumbnail_url text,                             -- description: Thumbnail URL (nullable)

  center geography(point, 4326) not null,         -- description: Center point (WGS84, geography)
  polygons geometry(multipolygon, 4326) not null, -- description: Boundary polygons (WGS84, geometry)

  created_at timestamptz not null default now(),  -- description: Row creation time
  updated_at timestamptz not null default now(),  -- description: Row last update time

  constraint geo_regions_unique_group_adm2 unique (group_id, adm2_id) -- description: Prevent duplicates per group
);

comment on table public.geo_regions is 'Geo regions (ADM2-level like city/ward) with PostGIS center and boundary.';
comment on column public.geo_regions.id is 'Internal primary key (UUID).';
comment on column public.geo_regions.group_id is 'Parent geo_region_groups.id.';
comment on column public.geo_regions.adm2_id is 'ADM2-level identifier used by the app/data source.';
comment on column public.geo_regions.name is 'Default display name.';
comment on column public.geo_regions.name_en is 'English name if available.';
comment on column public.geo_regions.name_ja is 'Japanese name if available.';
comment on column public.geo_regions.wikipedia is 'Wikipedia tag/value.';
comment on column public.geo_regions.iso3166_2 is 'ISO 3166-2 code.';
comment on column public.geo_regions.thumbnail_url is 'Thumbnail URL or Storage public URL.';
comment on column public.geo_regions.center is 'Center point (geography, SRID 4326).';
comment on column public.geo_regions.polygons is 'Boundary polygons (geometry MultiPolygon, SRID 4326).';
comment on column public.geo_regions.created_at is 'Creation timestamp.';
comment on column public.geo_regions.updated_at is 'Last updated timestamp.';

-- RLS
alter table public.geo_region_groups enable row level security;
alter table public.geo_regions enable row level security;

-- 既存ポリシーがあれば整理（任意）
drop policy if exists "geo_region_groups_select_public" on public.geo_region_groups;
drop policy if exists "geo_regions_select_public" on public.geo_regions;

-- SELECT: public (anon + authenticated)
create policy "geo_region_groups_select_public"
on public.geo_region_groups
for select
to anon, authenticated
using (true);

create policy "geo_regions_select_public"
on public.geo_regions
for select
to anon, authenticated
using (true);