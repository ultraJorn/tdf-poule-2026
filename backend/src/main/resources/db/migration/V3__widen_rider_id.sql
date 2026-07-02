-- Rider IDs added via the admin UI are {5-char-code}-r-{UUID} = 44 characters,
-- which overflows the original VARCHAR(40). Widen to 60 to give room.
ALTER TABLE rider ALTER COLUMN id TYPE VARCHAR(60);
