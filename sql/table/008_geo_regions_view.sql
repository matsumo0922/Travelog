-- TABLE (VIEW)
create or replace view public.geo_regions_view as
select
  id,
  group_id,
  adm2_id,
  name,
  name_en,
  name_ja,
  wikipedia,
  iso3166_2,
  thumbnail_url,
  ST_AsGeoJSON(center::geometry) as center_geojson,
  ST_AsGeoJSON(polygons) as polygons_geojson,
  created_at,
  updated_at
from public.geo_regions;

comment on view public.geo_regions_view is 'geo_regions with center/polygons converted to GeoJSON strings for client consumption.';
