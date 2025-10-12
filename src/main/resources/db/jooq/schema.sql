CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users
(
  user_id     uuid      DEFAULT uuid_generate_v4() NOT NULL
    PRIMARY KEY,
  provider    VARCHAR(50)                          NOT NULL,
  provider_id VARCHAR(255)                         NOT NULL,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (provider, provider_id)
);

CREATE TABLE refresh_token
(
  refresh_token_id uuid      DEFAULT uuid_generate_v4() NOT NULL
    PRIMARY KEY,
  user_id          uuid                                 NOT NULL
    CONSTRAINT fk_refresh_token_user
      REFERENCES users
      ON DELETE CASCADE,
  token            VARCHAR(1000)                        NOT NULL
    CONSTRAINT uq_refresh_token_token
      UNIQUE,
  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE images
(
  image_id           uuid      DEFAULT uuid_generate_v4() NOT NULL
    PRIMARY KEY,
  user_id            uuid                                 NOT NULL
    CONSTRAINT fk_images_user
      REFERENCES users
      ON DELETE CASCADE,
  original_file_name VARCHAR(255)                         NOT NULL,
  stored_file_name   VARCHAR(255)                         NOT NULL,
  file_path          VARCHAR(500)                         NOT NULL,
  file_size          BIGINT                               NOT NULL,
  content_type       VARCHAR(100)                         NOT NULL,
  created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
  updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
  deleted_at         TIMESTAMP
);

CREATE TABLE goals
(
  goal_id         uuid      DEFAULT uuid_generate_v4()          NOT NULL
    PRIMARY KEY,
  user_id         uuid                                          NOT NULL
    CONSTRAINT fk_goals_user
      REFERENCES users
      ON DELETE CASCADE,
  title           VARCHAR(500)                                  NOT NULL,
  description     TEXT,
  target_date     TIMESTAMP                                     NOT NULL,
  is_ai_generated BOOLEAN   DEFAULT FALSE                       NOT NULL,
  status          VARCHAR   DEFAULT 'ACTIVE'::CHARACTER VARYING NOT NULL,
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP           NOT NULL,
  updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP           NOT NULL,
  deleted_at      TIMESTAMP,
  image_id        uuid
    CONSTRAINT fk_goals_image
      REFERENCES images
      ON DELETE SET NULL
);

CREATE TABLE schedules
(
  schedule_id uuid        DEFAULT uuid_generate_v4()         NOT NULL
    PRIMARY KEY,
  goal_id     uuid                                           NOT NULL
    CONSTRAINT fk_schedules_goal
      REFERENCES goals
      ON DELETE CASCADE,
  user_id     uuid                                           NOT NULL
    CONSTRAINT fk_schedules_user
      REFERENCES users
      ON DELETE CASCADE,
  title       VARCHAR(255)                                   NOT NULL,
  description TEXT,
  status      VARCHAR(20) DEFAULT 'READY'::CHARACTER VARYING NOT NULL,
  start_date  TIMESTAMP                                      NOT NULL,
  end_date    TIMESTAMP                                      NOT NULL,
  created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP          NOT NULL,
  updated_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP          NOT NULL,
  deleted_at  TIMESTAMP,
  CONSTRAINT chk_schedule_dates
    CHECK (start_date <= end_date)
);

CREATE TABLE spring_ai_chat_memory
(
  conversation_id VARCHAR(255)  NOT NULL NOT NULL,
  message_index   SERIAL        NOT NULL NOT NULL,
  content         VARCHAR(1000) NOT NULL NOT NULL,
  type            VARCHAR(50)   NOT NULL NOT NULL,
  timestamp       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (conversation_id, message_index)
);

CREATE TABLE conversations
(
  conversation_id uuid      DEFAULT uuid_generate_v4() NOT NULL
    PRIMARY KEY,
  user_id         uuid                                 NOT NULL
    CONSTRAINT fk_conversations_user
      REFERENCES users
      ON DELETE CASCADE,
  goal_id         uuid
    CONSTRAINT fk_conversations_goal
      REFERENCES goals
      ON DELETE SET NULL,
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP  NOT NULL,
  deleted_at      TIMESTAMP
);

CREATE TABLE chat
(
  chat_id         UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
  conversation_id UUID             NOT NULL,
  message_index   BIGSERIAL        NOT NULL,
  content         TEXT             NOT NULL,
  type            VARCHAR(50)      NOT NULL,
  created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

