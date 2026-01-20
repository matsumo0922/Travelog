-- =============================================================================
-- upsert_geo_areas_batch: Batch upsert geo_areas
-- =============================================================================
-- Processes a JSONB array of areas and upserts them.
-- Returns array of {adm_id, id} mappings.
--
-- Input format (JSONB array):
-- [
--   {
--     "parent_id": "uuid-string" or null,
--     "level": 0,
--     "adm_id": "JP",
--     "country_code": "JP",
--     "name": "Japan",
--     "name_en": "Japan",
--     "name_ja": "日本",
--     "iso_code": "JP",
--     "wikipedia": "ja:日本",
--     "thumbnail_url": "https://...",
--     "center_lat": 35.6762,
--     "center_lon": 139.6503,
--     "polygons_geojson": {"type": "MultiPolygon", "coordinates": [...]}
--   },
--   ...
-- ]
-- =============================================================================

CREATE OR REPLACE FUNCTION public.upsert_geo_areas_batch(
  p_areas JSONB
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, extensions
AS $$
DECLARE
  v_area RECORD;
  v_result JSONB := '[]'::JSONB;
  v_area_id UUID;
  v_center GEOGRAPHY(POINT, 4326);
  v_polygons GEOMETRY(MULTIPOLYGON, 4326);
BEGIN
  -- Process each area in the batch
  FOR v_area IN
    SELECT
      (a->>'parent_id')::UUID AS parent_id,
      (a->>'level')::SMALLINT AS level,
      a->>'adm_id' AS adm_id,
      a->>'country_code' AS country_code,
      a->>'name' AS name,
      a->>'name_en' AS name_en,
      a->>'name_ja' AS name_ja,
      a->>'iso_code' AS iso_code,
      a->>'wikipedia' AS wikipedia,
      a->>'thumbnail_url' AS thumbnail_url,
      (a->>'center_lat')::DOUBLE PRECISION AS center_lat,
      (a->>'center_lon')::DOUBLE PRECISION AS center_lon,
      a->'polygons_geojson' AS polygons_geojson
    FROM jsonb_array_elements(p_areas) AS a
  LOOP
    -- Build center point
    v_center := NULL;
    IF v_area.center_lat IS NOT NULL AND v_area.center_lon IS NOT NULL THEN
      v_center := ST_SetSRID(ST_MakePoint(v_area.center_lon, v_area.center_lat), 4326)::GEOGRAPHY;
    END IF;

    -- Build polygons
    v_polygons := NULL;
    IF v_area.polygons_geojson IS NOT NULL THEN
      v_polygons := ST_SetSRID(ST_GeomFromGeoJSON(v_area.polygons_geojson::TEXT), 4326);
    END IF;

    -- Upsert
    INSERT INTO public.geo_areas (
      parent_id, level, adm_id, country_code,
      name, name_en, name_ja,
      iso_code, wikipedia, thumbnail_url,
      center, polygons
    )
    VALUES (
      v_area.parent_id, v_area.level, v_area.adm_id, v_area.country_code,
      v_area.name, v_area.name_en, v_area.name_ja,
      v_area.iso_code, v_area.wikipedia, v_area.thumbnail_url,
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

    -- Append result
    v_result := v_result || jsonb_build_object(
      'adm_id', v_area.adm_id,
      'id', v_area_id
    );
  END LOOP;

  RETURN v_result;
END;
$$;

COMMENT ON FUNCTION public.upsert_geo_areas_batch IS 'Batch upsert geo_areas. Returns array of {adm_id, id} mappings.';
