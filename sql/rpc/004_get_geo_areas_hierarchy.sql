-- =============================================================================
-- Geo Areas Hierarchy Query Functions
-- =============================================================================

-- -----------------------------------------------------------------------------
-- get_geo_areas_by_country: Get all areas for a country
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_geo_areas_by_country(
  p_country_code TEXT,
  p_max_level SMALLINT DEFAULT NULL
)
RETURNS SETOF public.geo_areas_view
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT *
  FROM public.geo_areas_view
  WHERE country_code = p_country_code
    AND (p_max_level IS NULL OR level <= p_max_level)
  ORDER BY level, name;
$$;

COMMENT ON FUNCTION public.get_geo_areas_by_country IS 'Get all geo areas for a country, optionally limited by level.';

-- -----------------------------------------------------------------------------
-- get_geo_area_children: Get direct children of a specific area
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_geo_area_children(
  p_parent_id UUID
)
RETURNS SETOF public.geo_areas_view
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT *
  FROM public.geo_areas_view
  WHERE parent_id = p_parent_id
  ORDER BY name;
$$;

COMMENT ON FUNCTION public.get_geo_area_children IS 'Get direct children of a geo area.';

-- -----------------------------------------------------------------------------
-- get_geo_area_descendants: Get area with all descendants (recursive)
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_geo_area_descendants(
  p_area_id UUID,
  p_max_depth SMALLINT DEFAULT 10
)
RETURNS SETOF public.geo_areas_view
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  WITH RECURSIVE descendants AS (
    SELECT *, 0 AS depth
    FROM public.geo_areas
    WHERE id = p_area_id

    UNION ALL

    SELECT ga.*, d.depth + 1
    FROM public.geo_areas ga
    JOIN descendants d ON ga.parent_id = d.id
    WHERE d.depth < p_max_depth
  )
  SELECT
    id, parent_id, level, adm_id, country_code,
    name, name_en, name_ja, iso_code, wikipedia, thumbnail_url,
    ST_AsGeoJSON(center::geometry) AS center_geojson,
    ST_AsGeoJSON(polygons) AS polygons_geojson,
    created_at, updated_at
  FROM descendants
  ORDER BY level, name;
$$;

COMMENT ON FUNCTION public.get_geo_area_descendants IS 'Get all descendants of a geo area recursively.';

-- -----------------------------------------------------------------------------
-- get_geo_area_ancestors: Get ancestors of an area (path to root)
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_geo_area_ancestors(
  p_area_id UUID
)
RETURNS SETOF public.geo_areas_view
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  WITH RECURSIVE ancestors AS (
    SELECT *
    FROM public.geo_areas
    WHERE id = p_area_id

    UNION ALL

    SELECT ga.*
    FROM public.geo_areas ga
    JOIN ancestors a ON ga.id = a.parent_id
  )
  SELECT
    id, parent_id, level, adm_id, country_code,
    name, name_en, name_ja, iso_code, wikipedia, thumbnail_url,
    ST_AsGeoJSON(center::geometry) AS center_geojson,
    ST_AsGeoJSON(polygons) AS polygons_geojson,
    created_at, updated_at
  FROM ancestors
  ORDER BY level;
$$;

COMMENT ON FUNCTION public.get_geo_area_ancestors IS 'Get all ancestors of a geo area (path to root).';

-- -----------------------------------------------------------------------------
-- get_geo_areas_by_level: Get all areas at a specific level for a country
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_geo_areas_by_level(
  p_country_code TEXT,
  p_level SMALLINT
)
RETURNS SETOF public.geo_areas_view
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT *
  FROM public.geo_areas_view
  WHERE country_code = p_country_code
    AND level = p_level
  ORDER BY name;
$$;

COMMENT ON FUNCTION public.get_geo_areas_by_level IS 'Get all geo areas at a specific level for a country.';

-- -----------------------------------------------------------------------------
-- get_distinct_country_codes: Get all available country codes
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_distinct_country_codes()
RETURNS TABLE(country_code TEXT)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT DISTINCT country_code
  FROM public.geo_areas
  WHERE level = 0
  ORDER BY country_code;
$$;

COMMENT ON FUNCTION public.get_distinct_country_codes IS 'Get all available country codes.';
