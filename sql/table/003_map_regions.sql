-- TABLE
CREATE TABLE map_regions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    map_id UUID NOT NULL REFERENCES maps(id) ON DELETE CASCADE,
    boundary_external_id TEXT NOT NULL,
    representative_image_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (map_id, boundary_external_id)
);

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
