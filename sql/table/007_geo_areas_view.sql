-- =============================================================================
-- geo_areas_view: View with geometry columns converted to GeoJSON strings
-- =============================================================================
-- This view is used for client consumption, converting PostGIS geometry/geography
-- columns to GeoJSON strings that can be easily parsed in Kotlin.
-- =============================================================================

CREATE OR REPLACE VIEW public.geo_areas_view AS
SELECT
  id,
  parent_id,
  level,
  adm_id,
  country_code,
  name,
  name_en,
  name_ja,
  iso_code,
  wikipedia,
  thumbnail_url,
  ST_AsGeoJSON(center::geometry) AS center_geojson,
  ST_AsGeoJSON(polygons) AS polygons_geojson,
  created_at,
  updated_at
FROM public.geo_areas;

COMMENT ON VIEW public.geo_areas_view IS 'geo_areas with geometry columns converted to GeoJSON strings for client consumption.';
