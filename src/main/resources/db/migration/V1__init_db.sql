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
                               high_profit DOUBLE PRECISION NOT NULL,
                               low_profit DOUBLE PRECISION NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE bot (
                     id BIGSERIAL PRIMARY KEY,
                     name VARCHAR(100) NOT NULL,
                     market_pair VARCHAR(50) NOT NULL,
                     is_running BOOLEAN default false,
                     is_reinvest BOOLEAN default false,
                     deadline_minutes int NOT NULL,
                     deposit DOUBLE PRECISION NOT NULL,
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
                        type VARCHAR(20) NOT NULL,
                        quantity DOUBLE PRECISION NOT NULL,
                        price DOUBLE PRECISION NOT NULL,
                        status VARCHAR(50) NOT NULL,
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

INSERT INTO profit_config (high_profit, low_profit)
VALUES (1, 0.3),
       (1.5, 0.5),
       (2, 0.8);


INSERT INTO strategy (name, description)
VALUES ('WGP', 'Wait good price strategy'),
       ('ECP', 'Enter current price strategy'),
       ('CPBV', 'Buy vector, enter current price strategy');

INSERT INTO bot (name, market_pair, is_running, deadline_minutes, deposit, strategy_id, profit_config_id, is_reinvest)
VALUES ('SOL/USDT bot', 'SOLUSDT', false, 3, 150.0, 2, 2, true),
       ('BTC/USDT bot', 'BTCUSDT', false, 3, 200.0, 1, 1, false),
       ('ETH/USDT bot', 'ETHUSDT', false, 3, 180.0, 1, 1, false),
       ('ADA/USDT bot', 'ADAUSDT', false, 3, 120.0, 2, 2, true),
       ('BNB/USDT bot', 'BNBUSDT', false, 3, 130.0, 1, 1, false),
       ('XRP/USDT bot', 'XRPUSDT', false, 3, 120.0, 2, 3, false);




