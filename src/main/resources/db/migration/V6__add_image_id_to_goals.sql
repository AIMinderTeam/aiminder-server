ALTER TABLE goals
    ADD COLUMN image_id UUID;

ALTER TABLE goals
    ADD CONSTRAINT fk_goals_image
        FOREIGN KEY (image_id)
            REFERENCES images (image_id)
            ON DELETE SET NULL;

CREATE INDEX idx_goals_image_id ON goals (image_id);