CREATE TABLE strategy (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(50) NOT NULL,
                          description TEXT
);

CREATE TABLE market_condition (
                                  id BIGSERIAL PRIMARY KEY,
                                  long_term_rsi DOUBLE PRECISION NOT NULL,
                                  short_term_rsi DOUBLE PRECISION NOT NULL,
                                  price DOUBLE PRECISION NOT NULL,
                                  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE profit_config (
                               id BIGSERIAL PRIMARY KEY,
                               profit_percentage DOUBLE PRECISION NOT NULL,
                               order_offset DOUBLE PRECISION NOT NULL,
                               high_profit_threshold DOUBLE PRECISION,
                               low_profit_threshold DOUBLE PRECISION,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE bot (
                     id BIGSERIAL PRIMARY KEY,
                     name VARCHAR(100) NOT NULL,
                     market_pair VARCHAR(50) NOT NULL,
                     is_running BOOLEAN default false,
                     deadline_minutes int NOT NULL,
                     deposit BIGINT NOT NULL,
                     strategy_id BIGINT REFERENCES strategy(id),
                     profit_config_id BIGINT REFERENCES profit_config(id),
                     market_condition_id BIGINT REFERENCES market_condition(id),
                     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                     updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE rsi_indicator (
                               id BIGSERIAL PRIMARY KEY,
                               bot_id BIGINT REFERENCES bot(id),
                               time_period INTEGER NOT NULL,
                               rsi_value DOUBLE PRECISION NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE trade (
                       id BIGSERIAL PRIMARY KEY,
                       bot_id BIGINT REFERENCES bot(id),
                       status VARCHAR(50) NOT NULL,
                       buy_price DOUBLE PRECISION NOT NULL,
                       sell_price DOUBLE PRECISION NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       completed_at TIMESTAMP
);

CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        symbol VARCHAR(50) NOT NULL,
                        trade_id BIGINT REFERENCES trade(id),
                        bot_id BIGINT REFERENCES bot(id),
                        type VARCHAR(20) NOT NULL, -- Тип ордера: BUY или SELL
                        quantity DOUBLE PRECISION NOT NULL, -- Количество
                        price DOUBLE PRECISION NOT NULL, -- Цена ордера
                        status VARCHAR(50) NOT NULL, -- Статус: PENDING, FILLED, CANCELLED
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE trade_history (
                               id BIGSERIAL PRIMARY KEY,
                               trade_id BIGINT REFERENCES trade(id),
                               bot_id BIGINT REFERENCES bot(id),
                               details TEXT,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO strategy (name, description) VALUES
                                             ('LONG', 'Long-term buying strategy'),
                                             ('SHORT', 'Short-term selling strategy');
INSERT INTO profit_config (profit_percentage, order_offset, high_profit_threshold, low_profit_threshold)
VALUES (0.2, 0.5, 1.5, 0.3);




