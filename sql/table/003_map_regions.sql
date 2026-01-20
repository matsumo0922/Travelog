-- =============================================================================
-- map_regions: Selected regions within a map
-- =============================================================================
-- Each map_region links a map to a specific geo_area (region/city).
-- The geo_area_id references the geo_areas table for geographical data.
-- =============================================================================

-- TABLE
CREATE TABLE map_regions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    map_id UUID NOT NULL REFERENCES maps(id) ON DELETE CASCADE,

    -- FK reference to geo_areas (replaces TEXT-based boundary_external_id)
    geo_area_id UUID NOT NULL REFERENCES geo_areas(id) ON DELETE RESTRICT,

    representative_image_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (map_id, geo_area_id)
);

-- Comments
COMMENT ON TABLE map_regions IS 'Selected regions within a map, referencing geo_areas.';
COMMENT ON COLUMN map_regions.id IS 'Primary key UUID.';
COMMENT ON COLUMN map_regions.map_id IS 'Parent map UUID.';
COMMENT ON COLUMN map_regions.geo_area_id IS 'Referenced geo_area UUID.';
COMMENT ON COLUMN map_regions.representative_image_id IS 'Optional representative image UUID.';

-- Indexes
CREATE INDEX IF NOT EXISTS idx_map_regions_map_id ON map_regions(map_id);
CREATE INDEX IF NOT EXISTS idx_map_regions_geo_area_id ON map_regions(geo_area_id);

-- RLS
ALTER TABLE map_regions ENABLE ROW LEVEL SECURITY;

CREATE POLICY map_regions_owner_all
ON map_regions
FOR ALL
USING (
    EXISTS (
        SELECT 1 FROM maps
        WHERE maps.id = map_regions.map_id
          AND maps.owner_user_id = auth.uid()
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1 FROM maps
        WHERE maps.id = map_regions.map_id
          AND maps.owner_user_id = auth.uid()
    )
);
