ALTER TABLE refresh_token
  RENAME COLUMN registered_date TO created_at;

ALTER TABLE refresh_token
  RENAME COLUMN modified_date TO updated_at;