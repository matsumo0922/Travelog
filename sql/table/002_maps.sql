-- =============================================================================
-- maps: User-created map collections
-- =============================================================================
-- Each map is owned by a user and references a root geo_area (country/region).
-- The root_geo_area_id provides the geographical scope for the map.
-- =============================================================================

-- TABLE
CREATE TABLE maps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- FK reference to geo_areas (replaces TEXT-based root_boundary_external_id)
    root_geo_area_id UUID NOT NULL REFERENCES geo_areas(id) ON DELETE RESTRICT,

    title TEXT NOT NULL,
    description TEXT NULL,
    icon_image_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Comments
COMMENT ON TABLE maps IS 'User-created map collections with geographical scope.';
COMMENT ON COLUMN maps.id IS 'Primary key UUID.';
COMMENT ON COLUMN maps.owner_user_id IS 'Owner user UUID.';
COMMENT ON COLUMN maps.root_geo_area_id IS 'Root geo_area UUID (country or region scope).';
COMMENT ON COLUMN maps.title IS 'Map title.';
COMMENT ON COLUMN maps.description IS 'Optional map description.';
COMMENT ON COLUMN maps.icon_image_id IS 'Optional icon image UUID.';

-- Indexes
CREATE INDEX IF NOT EXISTS idx_maps_owner_user_id ON maps(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_maps_root_geo_area_id ON maps(root_geo_area_id);

-- RLS
ALTER TABLE maps ENABLE ROW LEVEL SECURITY;

CREATE POLICY maps_owner_all
ON maps
FOR ALL
USING (owner_user_id = auth.uid())
WITH CHECK (owner_user_id = auth.uid());
