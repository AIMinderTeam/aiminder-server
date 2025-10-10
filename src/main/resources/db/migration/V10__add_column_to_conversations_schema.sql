ALTER TABLE conversations
  ADD COLUMN goal_id UUID;

ALTER TABLE conversations
  ADD CONSTRAINT fk_conversations_goal
    FOREIGN KEY (goal_id)
      REFERENCES goals (goal_id) ON DELETE SET NULL;
