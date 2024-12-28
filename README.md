# Project: Trade Bot

## Overview

The **Trade Bot** is a robust application designed for cryptocurrency trading, utilizing technical indicators and strategies to automate trading on Binance. The bot is designed to manage multiple trading pairs, perform market analysis, and execute trades based on predefined strategies.

---

## Key Features

- **Automated Trading:** Executes buy/sell orders based on market conditions.
- **Technical Analysis:** Utilizes indicators like RSI, EMA, and ATR to make trading decisions.
- **Multi-Pair Support:** Allows trading on multiple cryptocurrency pairs simultaneously.
- **Dynamic Configuration:** Supports flexible profit strategies and risk management.
- **Reinvestment Logic:** Automatically reinvests profits into trading capital.
- **Database Integration:** Uses PostgreSQL for storing market conditions, trade history, and configuration data.
- **Swagger API Documentation:** Provides a RESTful API interface with Swagger for easy interaction.

---

## Technologies Used

- **Java 17**: Core language for backend development.
- **Spring Boot**: Framework for creating REST APIs and managing the application lifecycle.
- **Hibernate (JPA)**: ORM tool for database interaction.
- **PostgreSQL**: Relational database for data persistence.
- **MapStruct**: Used for mapping models to DTOs and vice versa.
- **Swagger/OpenAPI**: For documenting and testing API endpoints.
- **Binance API**: For live trading and market data retrieval.
- **Maven**: Build automation and dependency management.
- **Docker**: For containerized deployment.

---

## Setup Instructions

### Prerequisites

1. **Java 17**: Ensure Java 17 is installed on your system.
2. **Maven**: Install Maven for dependency management.
3. **PostgreSQL**: Set up a PostgreSQL database.
4. **Docker** (Optional): For containerized deployment.

### Configuration

1. **Database Setup**:

    - Create a PostgreSQL database named `trade_bot`.

2. **Application Properties**: Create `src/main/resources/application.properties`:

   ```properties
    spring.application.name=trade.bot

    spring.datasource.url=your_database_url
    spring.datasource.username=your_username
    spring.datasource.password=your_password

    spring.flyway.enabled=true
    spring.flyway.baseline-on-migrate=true
    spring.flyway.locations=classpath:db/migration

    scheduler.update-interval=60000

    binance.api.key=your_binance_api_key
    binance.secret.key=your_binance_secret_key

    springdoc.api-docs.enabled=true
    springdoc.swagger-ui.enabled=true
    springdoc.swagger-ui.path=/swagger-ui.html
    springdoc.api-docs.path=/v3/api-docs
   ```

### Running the Application

1. Build the project:
   ```bash
   mvn clean install
   ```
2. Run the application:
   ```bash
   java -jar target/trade.bot-0.0.1.jar
   ```

### Docker Setup

1. Build the Docker image:
   ```bash
   docker build -t trade-bot .
   ```
2. Run the container:
   ```bash
   docker run -p 8080:8080 trade-bot
   ```

---

## API Documentation

### Swagger UI

The application provides Swagger UI for testing and interacting with the API.

- **URL:** [http://localhost:8080/swagger-ui/index.html#/](http://localhost:8080/swagger-ui/index.html#/)

### Key Endpoints

#### **Bot Management**

- `GET /api/bots` - List all bots.
- `POST /api/bots` - Create a new bot.
- `PUT /api/bots/{id}` - Update a bot.
- `GET /api/bots/{id}` - Get a bot.
- `DELETE /api/bots/{id}` - Delete a bot.

#### **Trade History**

- `GET /api/trade-history` - List all trade histories.
- `POST /api/trade-history` - Create a new trade history.
- `PUT /api/trade-history/{id}` - Update a trade history.
- `GET /api/trade-history/{id}` - Get a trade history.
- `DELETE /api/trade-history/{id}` - Delete a trade history.

#### **Trade**

- `GET /api/trades` - List all trades.
- `POST /api/trades` - Create a new trade.
- `PUT /api/trades/{id}` - Update a trade.
- `GET /api/trades/{id}` - Get a trade.
- `DELETE /api/trades/{id}` - Delete a trade.

#### **Orders**

- `GET /api/orders` - List all orders.
- `POST /api/orders` - Create a new order.
- `PUT /api/orders/{id}` - Update a order.
- `GET /api/orders/{id}` - Get a order.
- `DELETE /api/orders/{id}` - Delete a order.

---

## Technical Details

### Reinvestment Logic

The bot automatically reinvests profits:

- Splits profits equally between long and short strategies.
- Dynamically adjusts capital allocation.

### Indicators and Strategy

1. **RSI (Relative Strength Index):**
    - `longTermRSI`: Determines market direction.
    - `shortTermRSI`: Adjusts profit margins for trades.
2. **EMA (Exponential Moving Average):**
    - Used to confirm price trends.
3. **ATR (Average True Range):**
    - Assesses market volatility.

### Error Handling

- **LOT\_SIZE Errors:** Ensures order quantity matches Binance requirements.
- **MIN\_NOTIONAL Errors:** Validates minimum trade value.
- **Retry Logic:** Retries API calls on transient errors.

---

# Economic aspects
This document provides an economic perspective on the bot trading system, outlining its key features, financial mechanisms, and strategies to optimize profitability. The system is designed to maximize returns with minimal risk, leveraging automated trading bots that utilize strategic reinvestment and risk management techniques.

---

## Economic Objectives
1. **Maximizing Profitability**:
    - The bots aim to consistently generate profits by executing trades with calculated risk and predetermined profit margins.
    - Reinvestment strategies are employed to accelerate the growth of capital.

2. **Minimizing Risk**:
    - Trades are executed with small percentages of the total deposit to avoid significant losses.
    - Diversification across multiple trading pairs reduces exposure to market volatility.

3. **Efficient Capital Allocation**:
    - Deposits are distributed between trading pairs and adjusted based on market performance.
    - Profits are reinvested to scale the trading volume proportionally to the growth of the account.

---

## System Components
### 1. **Trading Pairs**
- Primary pairs currently include:
    - **BNB/USDT**: High liquidity, low fees, and stable volatility.
    - **ADA/USDT**: Moderate volatility with consistent trading opportunities.
    - **SOL/USDT**: High volatility, suitable for aggressive strategies.

### 2. **Profit Mechanisms**
- **Fixed Profit Target**:
    - Each bot is configured to close trades at a profit target (e.g., 2%).
    - This ensures consistent returns on every trade.

- **Reinvestment**:
    - Profits from trades are reinvested, increasing trade sizes and compounding returns.

### 3. **Risk Management**
- **Stop Loss Protection**:
    - Trades are closed automatically when losses exceed predefined thresholds.
- **Capital Allocation**:
    - Deposits are distributed evenly or weighted based on pair performance.

---

## Economic Strategies
### 1. **Short-Term Gains (24 Hours)**
- Target profit: ~1.5-2% daily.
- Focus on high-liquidity pairs (e.g., BNB/USDT).
- Frequent small trades to capture market micro-movements.

### 2. **Mid-Term Growth (1 Week)**
- Target profit: ~10-15% weekly.
- Diversified trading across 2-3 pairs to balance risk and reward.
- Adjust deposit allocations based on pair performance.

### 3. **Long-Term Scaling (1 Month)**
- Target profit: ~35-40% monthly.
- Utilize reinvestment to compound profits.
- Continuously monitor and replace underperforming pairs.

---

## Key Performance Metrics
1. **Daily Return on Investment (ROI):**
    - Measures the percentage profit generated daily relative to the total deposit.
2. **Profit/Loss per Pair:**
    - Tracks profitability for individual trading pairs to optimize allocations.
3. **Total Cumulative Profit:**
    - Calculates overall gains over specified time periods.

---

## Recommendations
1. **Monitor Performance Regularly:**
    - Review profitability metrics for each pair daily.
    - Replace or adjust settings for underperforming pairs.

2. **Limit Exposure:**
    - Avoid over-allocating to highly volatile pairs.
    - Use diversification to spread risk.

3. **Leverage Reinvestment Carefully:**
    - Ensure reinvestment does not exceed a safe percentage of total capital.

4. **Stay Updated with Market Conditions:**
    - Economic news and market sentiment can significantly impact volatility.
    - Adjust strategies to align with current trends.

---

## Conclusion
The bot system is designed to provide steady and scalable returns through disciplined trading strategies. By focusing on consistent profits, reinvestment, and effective risk management, the system ensures long-term financial growth. Regular monitoring and adjustments are key to maintaining optimal performance.
