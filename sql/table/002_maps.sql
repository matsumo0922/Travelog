-- TABLE
CREATE TABLE maps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    root_boundary_external_id TEXT NOT NULL,
    country_code TEXT NULL,
    title TEXT NOT NULL,
    description TEXT NULL,
    icon_image_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- RLS
ALTER TABLE maps ENABLE ROW LEVEL SECURITY;

CREATE POLICY maps_owner_all
ON maps
FOR ALL
USING (owner_user_id = auth.uid())
WITH CHECK (owner_user_id = auth.uid());
