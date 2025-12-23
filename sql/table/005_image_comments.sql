-- TABLE
CREATE TABLE image_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    image_id UUID NOT NULL REFERENCES images(id) ON DELETE CASCADE,
    author_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    deleted_at TIMESTAMPTZ NULL
);

-- RLS
ALTER TABLE image_comments ENABLE ROW LEVEL SECURITY;

CREATE POLICY image_comments_author_all
ON image_comments
FOR ALL
USING (author_user_id = auth.uid())
WITH CHECK (author_user_id = auth.uid());
