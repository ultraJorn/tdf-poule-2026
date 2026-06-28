-- Tour de France poule schema
-- One poule = one group's game: a roster of riders, 21 stages, and each
-- player's team. Mirrors the shape that used to live in the browser's
-- window.storage key/value store.

CREATE TABLE poule (
    id                   VARCHAR(8)    PRIMARY KEY,           -- the join code, e.g. "ABCDE"
    name                 VARCHAR(120)  NOT NULL,
    admin_password_hash  VARCHAR(100)  NOT NULL,
    budget_cap           INTEGER       NOT NULL,
    team_size            INTEGER       NOT NULL,
    total_swaps          INTEGER       NOT NULL,
    current_stage        INTEGER       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE rider (
    id        VARCHAR(40)   PRIMARY KEY,
    poule_id  VARCHAR(8)    NOT NULL REFERENCES poule(id) ON DELETE CASCADE,
    name      VARCHAR(120)  NOT NULL,
    team      VARCHAR(120)  NOT NULL,
    price     INTEGER       NOT NULL,
    tag       VARCHAR(40)   NOT NULL,
    active    BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_rider_poule ON rider(poule_id);

CREATE TABLE stage (
    id               BIGSERIAL     PRIMARY KEY,
    poule_id         VARCHAR(8)    NOT NULL REFERENCES poule(id) ON DELETE CASCADE,
    stage_number     INTEGER       NOT NULL,
    label            VARCHAR(160)  NOT NULL,
    stage_tag        VARCHAR(20)   NOT NULL,
    km               INTEGER,
    elev             INTEGER,
    note_en          TEXT,
    note_nl          TEXT,
    favorites        JSONB         NOT NULL DEFAULT '[]'::jsonb,
    locked           BOOLEAN       NOT NULL DEFAULT FALSE,
    top_rider_ids    JSONB         NOT NULL DEFAULT '[]'::jsonb,   -- ordered, up to 15 finish places
    gc_rider_ids     JSONB         NOT NULL DEFAULT '[]'::jsonb,   -- ordered, up to 10 GC places
    jerseys          JSONB         NOT NULL DEFAULT '{}'::jsonb,   -- {yellow,green,polka,white} -> riderId
    points_by_rider  JSONB         NOT NULL DEFAULT '{}'::jsonb,   -- riderId -> points scored that stage
    UNIQUE (poule_id, stage_number)
);

CREATE INDEX idx_stage_poule ON stage(poule_id);

CREATE TABLE player_team (
    id              BIGSERIAL     PRIMARY KEY,
    poule_id        VARCHAR(8)    NOT NULL REFERENCES poule(id) ON DELETE CASCADE,
    username        VARCHAR(60)   NOT NULL,
    rider_ids       JSONB         NOT NULL DEFAULT '[]'::jsonb,
    swaps_used      INTEGER       NOT NULL DEFAULT 0,
    swap_log        JSONB         NOT NULL DEFAULT '[]'::jsonb,    -- [{outId,inId,effectiveFromStage,ts}]
    joined_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_player_team_poule_username ON player_team(poule_id, lower(username));
