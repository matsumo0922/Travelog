-- =============================================================================
-- get_missing_names_count: Get count of areas with missing name_en or name_ja
-- =============================================================================
-- Returns the total count, count with missing name_en, and count with missing name_ja
-- for a specific country and optionally filtered by administrative level.
-- =============================================================================

CREATE OR REPLACE FUNCTION public.get_missing_names_count(
  p_country_code TEXT,
  p_level SMALLINT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_total INT;
  v_missing_name_en INT;
  v_missing_name_ja INT;
BEGIN
  IF p_level IS NOT NULL THEN
    SELECT COUNT(*) INTO v_total
    FROM public.geo_areas
    WHERE country_code = p_country_code AND level = p_level;

    SELECT COUNT(*) INTO v_missing_name_en
    FROM public.geo_areas
    WHERE country_code = p_country_code AND level = p_level AND name_en IS NULL;

    SELECT COUNT(*) INTO v_missing_name_ja
    FROM public.geo_areas
    WHERE country_code = p_country_code AND level = p_level AND name_ja IS NULL;
  ELSE
    SELECT COUNT(*) INTO v_total
    FROM public.geo_areas
    WHERE country_code = p_country_code;

    SELECT COUNT(*) INTO v_missing_name_en
    FROM public.geo_areas
    WHERE country_code = p_country_code AND name_en IS NULL;

    SELECT COUNT(*) INTO v_missing_name_ja
    FROM public.geo_areas
    WHERE country_code = p_country_code AND name_ja IS NULL;
  END IF;

  RETURN jsonb_build_object(
    'total', v_total,
    'missing_name_en', v_missing_name_en,
    'missing_name_ja', v_missing_name_ja
  );
END;
$$;

COMMENT ON FUNCTION public.get_missing_names_count IS 'Get count of areas with missing name_en or name_ja for a country, optionally filtered by level.';
