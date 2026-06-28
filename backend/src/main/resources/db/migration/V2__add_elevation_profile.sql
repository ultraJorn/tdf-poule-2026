-- Real route data: a downsampled distance/elevation profile parsed from each stage's GPX
-- file, plus the GPX-derived precise km/elev replacing the hand-typed approximations.
ALTER TABLE stage ADD COLUMN elevation_profile JSONB NOT NULL DEFAULT '[]'::jsonb;
