CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

create table flyway_schema_history
(
  installed_rank integer                 not null
    constraint flyway_schema_history_pk
      primary key,
  version        varchar(50),
  description    varchar(200)            not null,
  type           varchar(20)             not null,
  script         varchar(1000)           not null,
  checksum       integer,
  installed_by   varchar(100)            not null,
  installed_on   timestamp default now() not null,
  execution_time integer                 not null,
  success        boolean                 not null
);

create table users
(
  user_id     uuid      default uuid_generate_v4() not null
    primary key,
  provider    varchar(50)                          not null,
  provider_id varchar(255)                         not null,
  created_at  timestamp default CURRENT_TIMESTAMP,
  updated_at  timestamp default CURRENT_TIMESTAMP,
  unique (provider, provider_id)
);

create table refresh_token
(
  refresh_token_id uuid      default uuid_generate_v4() not null
    primary key,
  user_id          uuid                                 not null
    constraint fk_refresh_token_user
      references users
      on delete cascade,
  token            varchar(1000)                        not null
    constraint uq_refresh_token_token
      unique,
  created_at       timestamp default CURRENT_TIMESTAMP,
  updated_at       timestamp default CURRENT_TIMESTAMP
);

create table images
(
  image_id           uuid      default uuid_generate_v4() not null
    primary key,
  user_id            uuid                                 not null
    constraint fk_images_user
      references users
      on delete cascade,
  original_file_name varchar(255)                         not null,
  stored_file_name   varchar(255)                         not null,
  file_path          varchar(500)                         not null,
  file_size          bigint                               not null,
  content_type       varchar(100)                         not null,
  created_at         timestamp default CURRENT_TIMESTAMP  not null,
  updated_at         timestamp default CURRENT_TIMESTAMP  not null,
  deleted_at         timestamp
);

create table goals
(
  goal_id         uuid      default uuid_generate_v4()          not null
    primary key,
  user_id         uuid                                          not null
    constraint fk_goals_user
      references users
      on delete cascade,
  title           varchar(500)                                  not null,
  description     text,
  target_date     timestamp                                     not null,
  is_ai_generated boolean   default false                       not null,
  status          varchar   default 'ACTIVE'::character varying not null,
  created_at      timestamp default CURRENT_TIMESTAMP           not null,
  updated_at      timestamp default CURRENT_TIMESTAMP           not null,
  deleted_at      timestamp,
  image_id        uuid
    constraint fk_goals_image
      references images
      on delete set null
);

create table schedules
(
  schedule_id uuid        default uuid_generate_v4()         not null
    primary key,
  goal_id     uuid                                           not null
    constraint fk_schedules_goal
      references goals
      on delete cascade,
  user_id     uuid                                           not null
    constraint fk_schedules_user
      references users
      on delete cascade,
  title       varchar(255)                                   not null,
  description text,
  status      varchar(20) default 'READY'::character varying not null,
  start_date  timestamp                                      not null,
  end_date    timestamp                                      not null,
  created_at  timestamp   default CURRENT_TIMESTAMP          not null,
  updated_at  timestamp   default CURRENT_TIMESTAMP          not null,
  deleted_at  timestamp,
  constraint chk_schedule_dates
    check (start_date <= end_date)
);
