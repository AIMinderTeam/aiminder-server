create table public.flyway_schema_history
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

alter table public.flyway_schema_history
  owner to aiminder;

create index flyway_schema_history_s_idx
  on public.flyway_schema_history (success);

create table public.users
(
  user_id     uuid      default uuid_generate_v4() not null
    primary key,
  provider    varchar(50)                          not null,
  provider_id varchar(255)                         not null,
  created_at  timestamp default CURRENT_TIMESTAMP,
  updated_at  timestamp default CURRENT_TIMESTAMP,
  unique (provider, provider_id)
);

alter table public.users
  owner to aiminder;

create index idx_users_provider_id
  on public.users (provider, provider_id);

create table public.refresh_token
(
  refresh_token_id uuid      default uuid_generate_v4() not null
    primary key,
  user_id          uuid                                 not null
    constraint fk_refresh_token_user
      references public.users
      on delete cascade,
  token            text                                 not null
    constraint uq_refresh_token_token
      unique,
  created_at       timestamp default CURRENT_TIMESTAMP,
  updated_at       timestamp default CURRENT_TIMESTAMP
);

alter table public.refresh_token
  owner to aiminder;

create index idx_refresh_token_user_id
  on public.refresh_token (user_id);

create table public.images
(
  image_id           uuid      default uuid_generate_v4() not null
    primary key,
  user_id            uuid                                 not null
    constraint fk_images_user
      references public.users
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

alter table public.images
  owner to aiminder;

create table public.goals
(
  goal_id         uuid      default uuid_generate_v4()          not null
    primary key,
  user_id         uuid                                          not null
    constraint fk_goals_user
      references public.users
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
      references public.images
      on delete set null
);

alter table public.goals
  owner to aiminder;

create index idx_goals_user_id
  on public.goals (user_id);

create index idx_goals_status
  on public.goals (status);

create index idx_goals_deleted_at
  on public.goals (deleted_at)
  where (deleted_at IS NULL);

create index idx_goals_image_id
  on public.goals (image_id);

create index idx_images_user_id
  on public.images (user_id);

create index idx_images_created_at
  on public.images (created_at);

create index idx_images_deleted_at
  on public.images (deleted_at)
  where (deleted_at IS NULL);

create unique index idx_images_stored_file_name
  on public.images (stored_file_name)
  where (deleted_at IS NULL);

create table public.schedules
(
  schedule_id uuid        default uuid_generate_v4()         not null
    primary key,
  goal_id     uuid                                           not null
    constraint fk_schedules_goal
      references public.goals
      on delete cascade,
  user_id     uuid                                           not null
    constraint fk_schedules_user
      references public.users
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

alter table public.schedules
  owner to aiminder;

create index idx_schedules_user_id
  on public.schedules (user_id);

create index idx_schedules_goal_id
  on public.schedules (goal_id);

create index idx_schedules_status
  on public.schedules (status);

create index idx_schedules_dates
  on public.schedules (start_date, end_date);

create index idx_schedules_deleted_at
  on public.schedules (deleted_at)
  where (deleted_at IS NULL);

