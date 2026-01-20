-- =============================================================================
-- geo_areas: Unified geo areas table supporting ADM0-5 hierarchy
-- =============================================================================
-- Replaces the previous geo_region_groups (ADM1) and geo_regions (ADM2) tables
-- with a single self-referencing structure.
--
-- Hierarchy:
--   ADM0 (Country) -> ADM1 (Prefecture/State) -> ADM2 (City/District) -> ...
--
-- parent_id is NULL for ADM0 (root) entries.
-- =============================================================================

-- TABLE
CREATE TABLE IF NOT EXISTS public.geo_areas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  -- Hierarchy
  parent_id UUID REFERENCES public.geo_areas(id) ON DELETE CASCADE,
  level SMALLINT NOT NULL CHECK (level >= 0 AND level <= 5),

  -- Identifiers
  adm_id TEXT NOT NULL,
  country_code TEXT NOT NULL,  -- ISO 3166-1 alpha-2 (e.g., "JP", "US")

  -- Names
  name TEXT NOT NULL,
  name_en TEXT,
  name_ja TEXT,

  -- Metadata
  iso_code TEXT,          -- ISO 3166-2 code (for ADM1) or other ISO code
  wikipedia TEXT,         -- Wikipedia tag (e.g., "ja:東京都")
  thumbnail_url TEXT,

  -- Geography
  center GEOGRAPHY(POINT, 4326),
  polygons GEOMETRY(MULTIPOLYGON, 4326) NOT NULL,

  -- Timestamps
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  -- Constraints
  CONSTRAINT geo_areas_unique_parent_adm UNIQUE (parent_id, adm_id)
);

-- Comments
COMMENT ON TABLE public.geo_areas IS 'Unified geo areas table supporting ADM0-5 hierarchy with self-referencing parent_id.';
COMMENT ON COLUMN public.geo_areas.id IS 'Internal primary key (UUID).';
COMMENT ON COLUMN public.geo_areas.parent_id IS 'Parent area UUID. NULL for ADM0 (country) level.';
COMMENT ON COLUMN public.geo_areas.level IS 'Administrative level: 0=Country, 1=Prefecture/State, 2=City/District, etc.';
COMMENT ON COLUMN public.geo_areas.adm_id IS 'Administrative identifier from data source.';
COMMENT ON COLUMN public.geo_areas.country_code IS 'ISO 3166-1 alpha-2 country code.';
COMMENT ON COLUMN public.geo_areas.name IS 'Default display name.';
COMMENT ON COLUMN public.geo_areas.name_en IS 'English name if available.';
COMMENT ON COLUMN public.geo_areas.name_ja IS 'Japanese name if available.';
COMMENT ON COLUMN public.geo_areas.iso_code IS 'ISO code (e.g., ISO 3166-2 for ADM1).';
COMMENT ON COLUMN public.geo_areas.wikipedia IS 'Wikipedia tag/value for fetching thumbnails.';
COMMENT ON COLUMN public.geo_areas.thumbnail_url IS 'Thumbnail URL or Storage public URL.';
COMMENT ON COLUMN public.geo_areas.center IS 'Center point (geography, SRID 4326).';
COMMENT ON COLUMN public.geo_areas.polygons IS 'Boundary polygons (geometry MultiPolygon, SRID 4326).';
COMMENT ON COLUMN public.geo_areas.created_at IS 'Creation timestamp.';
COMMENT ON COLUMN public.geo_areas.updated_at IS 'Last updated timestamp.';

-- Indexes
CREATE INDEX IF NOT EXISTS idx_geo_areas_parent_id ON public.geo_areas(parent_id);
CREATE INDEX IF NOT EXISTS idx_geo_areas_country_code ON public.geo_areas(country_code);
CREATE INDEX IF NOT EXISTS idx_geo_areas_level ON public.geo_areas(level);
CREATE INDEX IF NOT EXISTS idx_geo_areas_country_level ON public.geo_areas(country_code, level);
CREATE INDEX IF NOT EXISTS idx_geo_areas_polygons_gist ON public.geo_areas USING GIST(polygons);
CREATE INDEX IF NOT EXISTS idx_geo_areas_center_gist ON public.geo_areas USING GIST(center);

-- RLS
ALTER TABLE public.geo_areas ENABLE ROW LEVEL SECURITY;

-- SELECT: public (anon + authenticated can read all geo areas)
CREATE POLICY "geo_areas_select_public"
ON public.geo_areas
FOR SELECT
TO anon, authenticated
USING (true);
