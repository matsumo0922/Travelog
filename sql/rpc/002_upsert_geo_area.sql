-- =============================================================================
-- upsert_geo_area: Upsert a single geo_area
-- =============================================================================
-- Inserts or updates a geo_area based on (parent_id, adm_id) uniqueness.
-- For root level (ADM0), parent_id is NULL.
-- Returns the area UUID.
-- =============================================================================

CREATE OR REPLACE FUNCTION public.upsert_geo_area(
  p_parent_id UUID,
  p_level SMALLINT,
  p_adm_id TEXT,
  p_country_code TEXT,
  p_name TEXT,
  p_name_en TEXT DEFAULT NULL,
  p_name_ja TEXT DEFAULT NULL,
  p_iso_code TEXT DEFAULT NULL,
  p_wikipedia TEXT DEFAULT NULL,
  p_thumbnail_url TEXT DEFAULT NULL,
  p_center_lat DOUBLE PRECISION DEFAULT NULL,
  p_center_lon DOUBLE PRECISION DEFAULT NULL,
  p_polygons_geojson JSONB DEFAULT NULL
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, extensions
AS $$
DECLARE
  v_area_id UUID;
  v_center GEOGRAPHY(POINT, 4326);
  v_polygons GEOMETRY(MULTIPOLYGON, 4326);
BEGIN
  -- Build center point if coordinates provided
  IF p_center_lat IS NOT NULL AND p_center_lon IS NOT NULL THEN
    v_center := ST_SetSRID(ST_MakePoint(p_center_lon, p_center_lat), 4326)::GEOGRAPHY;
  END IF;

  -- Build polygons from GeoJSON
  IF p_polygons_geojson IS NOT NULL THEN
    v_polygons := ST_SetSRID(ST_GeomFromGeoJSON(p_polygons_geojson::TEXT), 4326);
  END IF;

  -- Upsert based on (parent_id, adm_id)
  INSERT INTO public.geo_areas (
    parent_id, level, adm_id, country_code,
    name, name_en, name_ja,
    iso_code, wikipedia, thumbnail_url,
    center, polygons
  )
  VALUES (
    p_parent_id, p_level, p_adm_id, p_country_code,
    p_name, p_name_en, p_name_ja,
    p_iso_code, p_wikipedia, p_thumbnail_url,
    v_center, v_polygons
  )
  ON CONFLICT (parent_id, adm_id)
  DO UPDATE SET
    level = EXCLUDED.level,
    country_code = EXCLUDED.country_code,
    name = EXCLUDED.name,
    name_en = EXCLUDED.name_en,
    name_ja = EXCLUDED.name_ja,
    iso_code = EXCLUDED.iso_code,
    wikipedia = EXCLUDED.wikipedia,
    thumbnail_url = EXCLUDED.thumbnail_url,
    center = EXCLUDED.center,
    polygons = EXCLUDED.polygons,
    updated_at = NOW()
  RETURNING id INTO v_area_id;

  RETURN v_area_id;
END;
$$;

COMMENT ON FUNCTION public.upsert_geo_area IS 'Upsert a single geo_area. Returns the area UUID.';
