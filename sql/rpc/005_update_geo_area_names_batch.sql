-- =============================================================================
-- update_geo_area_names_batch: Batch update name_en and name_ja for geo_areas
-- =============================================================================
-- Updates name_en and name_ja for multiple areas in a single call.
-- Only updates non-null values (preserves existing values if not provided).
-- =============================================================================

CREATE OR REPLACE FUNCTION public.update_geo_area_names_batch(
  p_updates JSONB
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_update JSONB;
  v_id UUID;
  v_name_en TEXT;
  v_name_ja TEXT;
BEGIN
  FOR v_update IN SELECT * FROM jsonb_array_elements(p_updates)
  LOOP
    v_id := (v_update->>'id')::UUID;
    v_name_en := v_update->>'name_en';
    v_name_ja := v_update->>'name_ja';

    UPDATE public.geo_areas
    SET
      name_en = COALESCE(v_name_en, name_en),
      name_ja = COALESCE(v_name_ja, name_ja),
      updated_at = NOW()
    WHERE id = v_id;
  END LOOP;
END;
$$;

COMMENT ON FUNCTION public.update_geo_area_names_batch IS 'Batch update name_en and name_ja for geo_areas. Only updates non-null values.';
