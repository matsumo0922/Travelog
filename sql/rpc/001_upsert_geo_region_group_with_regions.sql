create or replace function public.upsert_geo_region_group_with_regions(
  p_adm_id text,
  p_adm_name text,
  p_regions jsonb
)
returns uuid
language plpgsql
as $$
declare
  v_group_id uuid;
begin
  -- upsert group
  insert into public.geo_region_groups (adm_id, adm_name)
  values (p_adm_id, p_adm_name)
  on conflict (adm_id)
  do update set adm_name = excluded.adm_name
  returning id into v_group_id;

  -- upsert regions
  insert into public.geo_regions (
    group_id,
    adm2_id,
    name,
    name_en,
    name_ja,
    wikipedia,
    iso3166_2,
    thumbnail_url,
    center,
    polygons
  )
  select
    v_group_id,
    r.adm2_id,
    r.name,
    r.name_en,
    r.name_ja,
    r.wikipedia,
    r.iso3166_2,
    r.thumbnail_url,
    ST_SetSRID(ST_MakePoint(r.center_lon, r.center_lat), 4326)::geography,
    ST_SetSRID(ST_GeomFromGeoJSON(r.polygons_geojson::text), 4326)
  from jsonb_to_recordset(p_regions) as r(
    adm2_id text,
    name text,
    name_en text,
    name_ja text,
    wikipedia text,
    iso3166_2 text,
    thumbnail_url text,
    center_lat double precision,
    center_lon double precision,
    polygons_geojson jsonb
  )
  on conflict (group_id, adm2_id)
  do update set
    name = excluded.name,
    name_en = excluded.name_en,
    name_ja = excluded.name_ja,
    wikipedia = excluded.wikipedia,
    iso3166_2 = excluded.iso3166_2,
    thumbnail_url = excluded.thumbnail_url,
    center = excluded.center,
    polygons = excluded.polygons;

  return v_group_id;
end;
$$;

comment on function public.upsert_geo_region_group_with_regions(text, text, jsonb)
is 'Upsert geo_region_groups by adm_id, then upsert geo_regions by (group_id, adm2_id). Converts lat/lon + GeoJSON into PostGIS geography/geometry.';
