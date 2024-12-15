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

CREATE TABLE bot (
                     id BIGSERIAL PRIMARY KEY,
                     name VARCHAR(100) NOT NULL,
                     market_pair VARCHAR(50) NOT NULL,
                     is_running BOOLEAN default false,
                     deposit BIGINT NOT NULL,
                     strategy_id BIGINT REFERENCES strategy(id) ON DELETE CASCADE,
                     profit_config_id BIGINT REFERENCES profit_config(id) ON DELETE CASCADE,
                     market_condition_id BIGINT REFERENCES market_condition(id) ON DELETE CASCADE,
                     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                     updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE profit_config (
                               id BIGSERIAL PRIMARY KEY,
                               profit_percentage DOUBLE PRECISION NOT NULL,
                               order_offset DOUBLE PRECISION NOT NULL,
                               high_profit_threshold DOUBLE PRECISION,
                               low_profit_threshold DOUBLE PRECISION,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE trade (
                       id BIGSERIAL PRIMARY KEY,
                       bot_id BIGINT REFERENCES bot(id) ON DELETE CASCADE,
                       order_id VARCHAR(100) NOT NULL,
                       type VARCHAR(10) CHECK (type IN ('BUY', 'SELL')) NOT NULL,
                       price DOUBLE PRECISION NOT NULL,
                       quantity DOUBLE PRECISION NOT NULL,
                       profit DOUBLE PRECISION,
                       status VARCHAR(20) CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')) DEFAULT 'PENDING',
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       completed_at TIMESTAMP
);

CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        trade_id BIGINT REFERENCES trade(id) ON DELETE CASCADE,
                        bot_id BIGINT REFERENCES bot(id) ON DELETE CASCADE,
                        type VARCHAR(10) NOT NULL, -- Тип ордера: BUY или SELL
                        quantity DOUBLE PRECISION NOT NULL, -- Количество
                        price DOUBLE PRECISION NOT NULL, -- Цена ордера
                        status VARCHAR(20) NOT NULL, -- Статус: PENDING, FILLED, CANCELLED
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE trade_history (
                               id BIGSERIAL PRIMARY KEY,
                               trade_id BIGINT REFERENCES trade(id) ON DELETE CASCADE,
                               bot_id BIGINT REFERENCES bot(id) ON DELETE CASCADE,
                               details TEXT,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE rsi_indicator (
                               id BIGSERIAL PRIMARY KEY,
                               bot_id BIGINT REFERENCES bot(id) ON DELETE CASCADE,
                               time_period INTEGER NOT NULL,
                               rsi_value DOUBLE PRECISION NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO strategy (name, description) VALUES
                                             ('LONG', 'Long-term buying strategy'),
                                             ('SHORT', 'Short-term selling strategy');

-- Заполнение ProfitConfig
INSERT INTO profit_config (profit_percentage, order_offset, high_profit_threshold, low_profit_threshold)
VALUES (0.7, 0.5, 30.0, 70.0);




