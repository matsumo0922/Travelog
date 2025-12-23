-- TABLE
CREATE TABLE users (
    id UUID PRIMARY KEY,
    handle TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    icon_image_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- RLS
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

CREATE POLICY users_select_own
ON users
FOR SELECT
USING (id = auth.uid());

CREATE POLICY users_update_own
ON users
FOR UPDATE
USING (id = auth.uid())
WITH CHECK (id = auth.uid());
