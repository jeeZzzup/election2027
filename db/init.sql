-- Schéma partagé par les deux API (l'API de collecte écrit, l'API de consultation lit).
-- Exécuté automatiquement au premier démarrage du conteneur PostgreSQL.

CREATE TABLE poll (
    id               BIGSERIAL PRIMARY KEY,
    external_key     VARCHAR(64)   NOT NULL UNIQUE,   -- empreinte SHA-1 : déduplication
    institute        VARCHAR(255)  NOT NULL,
    client           VARCHAR(255),
    fieldwork_start  DATE,
    fieldwork_end    DATE,
    sample_size      INTEGER,
    scenario         VARCHAR(255),                    -- hypothèse testée
    vote_round       INTEGER       NOT NULL DEFAULT 1, -- 1 = premier tour, 2 = second tour
    source_url       VARCHAR(1000),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_poll_fieldwork_end ON poll (fieldwork_end DESC);
CREATE INDEX idx_poll_institute ON poll (institute);

CREATE TABLE poll_result (
    id          BIGSERIAL PRIMARY KEY,
    poll_id     BIGINT        NOT NULL REFERENCES poll (id) ON DELETE CASCADE,
    candidate   VARCHAR(255)  NOT NULL,
    percentage  NUMERIC(5, 2) NOT NULL
);
CREATE INDEX idx_poll_result_poll ON poll_result (poll_id);
CREATE INDEX idx_poll_result_candidate ON poll_result (candidate);

CREATE TABLE article (
    id            BIGSERIAL PRIMARY KEY,
    external_key  VARCHAR(64)   NOT NULL UNIQUE,
    title         VARCHAR(500)  NOT NULL,
    url           VARCHAR(1500) NOT NULL,
    source        VARCHAR(255),
    published_at  TIMESTAMPTZ   NOT NULL,
    summary       VARCHAR(2000),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_article_published ON article (published_at DESC);

CREATE TABLE crawl_run (
    id           BIGSERIAL PRIMARY KEY,
    source       VARCHAR(100)  NOT NULL,
    started_at   TIMESTAMPTZ   NOT NULL,
    finished_at  TIMESTAMPTZ,
    status       VARCHAR(20)   NOT NULL,              -- RUNNING / SUCCESS / FAILED
    items_found  INTEGER       NOT NULL DEFAULT 0,
    items_new    INTEGER       NOT NULL DEFAULT 0,
    message      VARCHAR(2000)
);
CREATE INDEX idx_crawl_run_started ON crawl_run (started_at DESC);
