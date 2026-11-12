CREATE TABLE plans
(
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(30)  NOT NULL UNIQUE,
    name           VARCHAR(100) NOT NULL,
    price          BIGINT       NOT NULL CHECK (price >= 0),
    billing_period VARCHAR(20)  NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE subscriptions
(
    id                   BIGSERIAL PRIMARY KEY,
    member_id            BIGINT      NOT NULL,
    plan_id              BIGINT      NOT NULL REFERENCES plans (id),
    status               VARCHAR(20) NOT NULL,
    current_period_start TIMESTAMPTZ,
    current_period_end   TIMESTAMPTZ,
    cancel_at_period_end BOOLEAN     NOT NULL DEFAULT FALSE,
    canceled_at          TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    version              BIGINT      NOT NULL DEFAULT 0
);

-- A member can only have one subscription that is not finished yet.
-- The service checks this too, but two requests at the same time can both pass that check.
CREATE UNIQUE INDEX uk_subscriptions_member_live
    ON subscriptions (member_id)
    WHERE status IN ('PENDING', 'ACTIVE', 'PAST_DUE');

CREATE INDEX idx_subscriptions_period_end ON subscriptions (current_period_end) WHERE status = 'ACTIVE';

INSERT INTO plans (code, name, price, billing_period)
VALUES ('BASIC', 'Basic', 9900, 'MONTHLY'),
       ('PRO', 'Pro', 19900, 'MONTHLY');
