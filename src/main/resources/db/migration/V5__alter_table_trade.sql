ALTER TABLE trade
ADD COLUMN strategy_id BIGINT REFERENCES strategy(id),
ADD COLUMN trading_vector VARCHAR(50);