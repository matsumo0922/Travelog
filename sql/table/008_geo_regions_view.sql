-- TABLE
create or replace view public.geo_region_groups_view as
select
  id,
  adm_id,
  adm_name,
  ST_AsGeoJSON(polygons) as polygons_geojson,
  created_at,
  updated_at
from public.geo_region_groups;

comment on view public.geo_region_groups_view is 'geo_region_groups with polygons converted to GeoJSON string for client consumption.';
