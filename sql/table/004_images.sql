-- TABLE
CREATE TABLE images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uploader_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    map_region_id UUID NULL REFERENCES map_regions(id) ON DELETE CASCADE,
    storage_key TEXT NOT NULL,
    content_type TEXT NULL,
    file_size BIGINT NULL,
    width INTEGER NULL,
    height INTEGER NULL,
    taken_at TIMESTAMPTZ NULL,
    taken_lat DOUBLE PRECISION NULL,
    taken_lng DOUBLE PRECISION NULL,
    exif JSONB NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- RLS
ALTER TABLE images ENABLE ROW LEVEL SECURITY;

CREATE POLICY images_uploader_all
ON images
FOR ALL
USING (uploader_user_id = auth.uid())
WITH CHECK (uploader_user_id = auth.uid());
