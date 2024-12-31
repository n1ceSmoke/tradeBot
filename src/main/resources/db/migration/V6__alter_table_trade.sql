ALTER TABLE trade
ADD COLUMN strategy_id BIGINT REFERENCES strategy(id);