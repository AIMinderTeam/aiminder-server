CREATE TABLE images
(
    image_id           UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    user_id            UUID         NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name   VARCHAR(255) NOT NULL,
    file_path          VARCHAR(500) NOT NULL,
    file_size          BIGINT       NOT NULL,
    content_type       VARCHAR(100) NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at         TIMESTAMP,

    CONSTRAINT fk_images_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE INDEX idx_images_user_id ON images (user_id);
CREATE INDEX idx_images_created_at ON images (created_at);
CREATE INDEX idx_images_deleted_at ON images (deleted_at) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_images_stored_file_name ON images (stored_file_name) WHERE deleted_at IS NULL;