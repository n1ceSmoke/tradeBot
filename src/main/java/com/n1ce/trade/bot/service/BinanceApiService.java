package com.n1ce.trade.bot.service;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.*;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.exception.BinanceApiException;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class BinanceApiService {
	private final String BINANCE_API_BASE_URL = "https://api.binance.com/api/v3";

	private final BinanceApiRestClient binanceApiRestClient;
	private final RestTemplate restTemplate;
	private final UMFuturesClientImpl futuresClient;
	private final ObjectMapper objectMapper;

	public BinanceApiService(BinanceApiRestClient binanceApiRestClient, UMFuturesClientImpl futuresClient, ObjectMapper objectMapper) {
		this.binanceApiRestClient = binanceApiRestClient;
		this.futuresClient = futuresClient;
		this.restTemplate = new RestTemplate();
		this.objectMapper = objectMapper;
	}

	public double getCurrentPrice(String tradingPair) {
		String url = String.format("%s/ticker/price?symbol=%s", BINANCE_API_BASE_URL, tradingPair);

		try {
			ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				Object priceObject = response.getBody().get("price");
				if (priceObject != null) {
					return Double.parseDouble(priceObject.toString());
				} else {
					log.info("Failed to retrieve the current price from Binance API response");
					throw new RuntimeException("Failed to retrieve the current price from Binance API response: " + response.getStatusCode());
				}
			} else {
				log.info("Error fetching the current price from Binance API: " + response.getStatusCode());
				throw new RuntimeException("Error fetching the current price from Binance API: " + response.getStatusCode());
			}
		} catch (Exception e) {
			throw new RuntimeException("Error calling Binance API: " + e.getMessage(), e);
		}
	}

	public NewOrderResponse createOrder(String symbol, OrderSide side, String quantity, String price) {
		try {
			NewOrder order = new NewOrder(symbol, side, OrderType.LIMIT, TimeInForce.GTC, String.format(Locale.US, "%.8f", Double.parseDouble(quantity)));
			order.recvWindow(5000L);
			order.price(price);
			order.timestamp(getServerTime());
			NewOrderResponse orderResponse = binanceApiRestClient.newOrder(order);

			log.info("Order successfully created: " + orderResponse);

			return orderResponse;

		} catch (BinanceApiException e) {
			log.error("Failed to create order: " + e.getMessage());
			throw new RuntimeException("Failed to create order: " + e.getMessage());
		}
	}

	public boolean isOrderFilled(Long orderId, String symbol) {
		try {
			OrderStatusRequest request = new OrderStatusRequest(symbol, orderId);
			request.recvWindow(5000L);
			request.timestamp(getServerTime());
			Order order = binanceApiRestClient.getOrderStatus(request);

			return "FILLED".equalsIgnoreCase(order.getStatus().name());
		} catch (BinanceApiException e) {
			log.info("Error checking the status of the order: " + e.getMessage(), e);
			return false;
		}
	}

	public boolean isMarketOrderFilled(Long orderId, String symbol) {
		try {
			LinkedHashMap<String, Object> query = new LinkedHashMap<>();
			query.put("symbol", symbol);
			query.put("orderId", String.valueOf(orderId));
			query.put("timestamp", getFuturesServerTime());

			String response = futuresClient.account().queryOrder(query);
			JsonNode order = objectMapper.readTree(response);
			String status = order.get("status").asText();

			return "FILLED".equalsIgnoreCase(status);
		} catch (BinanceApiException e) {
			log.info("Error checking the status of the order: " + e.getMessage(), e);
			return false;
		} catch (JsonProcessingException e) {
			log.info("Error checking the status of the order: " + e.getMessage(), e);
			return false;
		}
	}

	public boolean cancelFuturesOrder(Long orderId, String symbol) {
		try {
			LinkedHashMap<String, Object> query = new LinkedHashMap<>();
			query.put("symbol", symbol);
			query.put("orderId", String.valueOf(orderId));
			query.put("timestamp", getFuturesServerTime());

			futuresClient.account().cancelOrder(query);
			return true;
		} catch (BinanceApiException e) {
			log.info("Error cancelling the order: " + e.getMessage());
			return false;
		}
	}

	public boolean cancelOrder(Long orderId, String symbol) {
		try {
			CancelOrderRequest request = new CancelOrderRequest(symbol, orderId);
			request.recvWindow(5000L);
			request.timestamp(getServerTime());
			binanceApiRestClient.cancelOrder(request);
			return true;
		} catch (BinanceApiException e) {
			log.info("Error cancelling the order: " + e.getMessage());
			return false;
		}
	}

	public double getAvailableBalance(String pair) {
		String asset = pair.replaceAll("USDT", "");
		Account account = binanceApiRestClient.getAccount();
		for (AssetBalance balance : account.getBalances()) {
			if (balance.getAsset().equals(asset)) {
				return new BigDecimal(balance.getFree()).doubleValue();
			}
		}
		return 0.0;
	}

	public List<Double> getPriceHistory(String symbol, int timePeriodMinutes) {
		CandlestickInterval interval = determineInterval(timePeriodMinutes);
		List<Candlestick> candlesticks = binanceApiRestClient.getCandlestickBars(symbol, interval, 300, null, null);

		return candlesticks.stream()
				.map(Candlestick::getClose)
				.map(Double::parseDouble)
				.collect(Collectors.toList());
	}

	public List<Candlestick> getCandlestickData(String symbol, CandlestickInterval interval) {
		return binanceApiRestClient.getCandlestickBars(symbol, interval, 1, null, null);
	}

	public List<Candlestick> getCandlestickDataWithLimit(String symbol, CandlestickInterval interval, int count) {
		return binanceApiRestClient.getCandlestickBars(symbol, interval, count, null, null);
	}

	public double adjustFuturesOrderQuantity(double orderQuantity, String symbol, double price) {
		Map<String, JsonNode> filterNodes = new HashMap<>();
		try {
			String response = futuresClient.market().exchangeInfo();
			try {
				JsonNode rootNode = objectMapper.readTree(response);
				JsonNode symbolsArray = rootNode.path("symbols");
				for (JsonNode symbolNode : symbolsArray) {
					if (symbolNode.path("symbol").asText().equals(symbol)) {
						JsonNode filters = symbolNode.path("filters");
						for (JsonNode filter : filters) {
							if (filter.path("filterType").asText().equals("LOT_SIZE") || filter.path("filterType").asText().equals("MIN_NOTIONAL")) {
								filterNodes.put(filter.path("filterType").asText(), filter);
							}
						}
					}
				}
			} catch (IOException e) {
				log.error("Error parsing Binance Futures JSON: ", e);
			}

			if (filterNodes.isEmpty()) {
				throw new IllegalArgumentException("Symbol information not found for: " + symbol);
			}

			JsonNode lotSizeFilter = filterNodes.get("LOT_SIZE");
			JsonNode minNotionalFilter = filterNodes.get("MIN_NOTIONAL");

			double stepSize = lotSizeFilter.get("stepSize").asDouble();
			double minQty = lotSizeFilter.get("minQty").asDouble();
			double adjustedQuantity = adjustOrderQuantity(orderQuantity, stepSize, minQty);

			if(null != minNotionalFilter) {
				double minNotional = minNotionalFilter.get("notional").asDouble();
				if (!isOrderValueValid(adjustedQuantity, price, minNotional)) {
					throw new IllegalArgumentException("Order value is less than the minimum notional allowed.");
				}

				log.info("Order Details - Symbol: {}, Quantity: {}, Step Size: {}, Min Qty: {}, Adjusted Quantity: {}, Min Notional: {}",
						symbol, orderQuantity, stepSize, minQty, adjustedQuantity, minNotional);
			}
			return adjustedQuantity;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public double adjustOrderQuantity(double orderQuantity, String symbol, double price) {
		ExchangeInfo exchangeInfo = binanceApiRestClient.getExchangeInfo();
		SymbolInfo symbolInfo = exchangeInfo.getSymbolInfo(symbol);

		if (symbolInfo == null) {
			throw new IllegalArgumentException("Symbol information not found for: " + symbol);
		}

		SymbolFilter lotSizeFilter = symbolInfo.getFilters().stream()
				.filter(filter -> filter.getFilterType().equals(FilterType.LOT_SIZE))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("LOT_SIZE filter not found"));

		SymbolFilter minNotionalFilter = symbolInfo.getFilters().stream()
				.filter(filter -> filter.getFilterType().equals(FilterType.MIN_NOTIONAL))
				.findFirst()
				.orElse(null);

		double stepSize = Double.parseDouble(lotSizeFilter.getStepSize());
		double minQty = Double.parseDouble(lotSizeFilter.getMinQty());
		double adjustedQuantity = adjustOrderQuantity(orderQuantity, stepSize, minQty);

		if(null != minNotionalFilter) {
			double minNotional = Double.parseDouble(minNotionalFilter.getMinNotional());
			if (!isOrderValueValid(adjustedQuantity, price, minNotional)) {
				throw new IllegalArgumentException("Order value is less than the minimum notional allowed.");
			}

			log.info("Order Details - Symbol: {}, Quantity: {}, Step Size: {}, Min Qty: {}, Adjusted Quantity: {}, Min Notional: {}",
					symbol, orderQuantity, stepSize, minQty, adjustedQuantity, minNotional);
		}
		return adjustedQuantity;
	}

	public double adjustOrderQuantity(double quantity, double stepSize, double minQty) {
		double adjustedQuantity = Math.floor(quantity / stepSize) * stepSize;

		if (adjustedQuantity < minQty) {
			adjustedQuantity = minQty;
		}
		adjustedQuantity = Math.round(adjustedQuantity * 1e8) / 1e8;

		return adjustedQuantity;
	}

	public boolean isOrderValueValid(double quantity, double price, double minNotional) {
		return (quantity * price) >= minNotional;
	}

	public long createFuturesOrder(String symbol, OrderSide side, double quantity) {
		try {
			LinkedHashMap<String, Object> order = new LinkedHashMap<>();
			order.put("symbol", symbol);
			order.put("side", side.toString());
			order.put("type", "MARKET");
			order.put("quantity", String.valueOf(quantity));
			order.put("timestamp", getFuturesServerTime());

			String response = futuresClient.account().newOrder(order);
			log.info("Futures order created: {}", response);
			JsonNode jsonResponse = objectMapper.readTree(response);
			return jsonResponse.get("orderId").asLong();
		} catch (Exception e) {
			log.error("Failed to create futures order for {}: {}", symbol, e.getMessage());
			throw new RuntimeException("Failed to create futures order");
		}
	}

	public void setLeverage(String symbol, int leverage) {
		try {
			LinkedHashMap<String, Object> leverageParams = new LinkedHashMap<>();
			leverageParams.put("symbol", symbol);
			leverageParams.put("leverage", leverage);
			leverageParams.put("timestamp", getFuturesServerTime());

			futuresClient.account().changeInitialLeverage(leverageParams);
			log.info("Leverage set to {}x for {}", leverage, symbol);
		} catch (Exception e) {
			log.error("Failed to set leverage for {}: {}", symbol, e.getMessage());
		}
	}

	public long createStopLossOrder(String symbol, OrderSide side, double quantity, double stopLossPrice) {
		try {
			OrderSide exitSide = (side == OrderSide.BUY) ? OrderSide.SELL : OrderSide.BUY;

			LinkedHashMap<String, Object> orderParams = new LinkedHashMap<>();
			orderParams.put("symbol", symbol);
			orderParams.put("side", exitSide.toString());
			orderParams.put("type", "STOP_MARKET");
			orderParams.put("quantity", String.valueOf(quantity));
			orderParams.put("stopPrice", String.valueOf(stopLossPrice));
			orderParams.put("reduceOnly", true);
			orderParams.put("timestamp", getFuturesServerTime());

			String response = futuresClient.account().newOrder(orderParams);
			log.info("Stop Loss order created: {}", response);

			JsonNode jsonResponse = objectMapper.readTree(response);
			return jsonResponse.get("orderId").asLong();
		} catch (Exception e) {
			log.error("Failed to create stop loss order for {}: {}", symbol, e.getMessage());
			throw new RuntimeException("Failed to create stop loss order");
		}
	}

	public long createTakeProfitOrder(String symbol, OrderSide side, double quantity, double takeProfitPrice) {
		try {
			OrderSide exitSide = (side == OrderSide.BUY) ? OrderSide.SELL : OrderSide.BUY;

			LinkedHashMap<String, Object> orderParams = new LinkedHashMap<>();
			orderParams.put("symbol", symbol);
			orderParams.put("side", exitSide.toString());
			orderParams.put("type", "TAKE_PROFIT_MARKET");
			orderParams.put("quantity", String.valueOf(quantity));
			orderParams.put("stopPrice", String.valueOf(takeProfitPrice));
			orderParams.put("reduceOnly", true);
			orderParams.put("timestamp", getFuturesServerTime());

			String response = futuresClient.account().newOrder(orderParams);
			log.info("Take Profit order created: {}", response);

			JsonNode jsonResponse = objectMapper.readTree(response);
			return jsonResponse.get("orderId").asLong();
		} catch (Exception e) {
			log.error("Failed to create take profit order for {}: {}", symbol, e.getMessage());
			throw new RuntimeException("Failed to create take profit order");
		}
	}


	private CandlestickInterval determineInterval(int timePeriodMinutes) {
		if (timePeriodMinutes <= 3) {
			return CandlestickInterval.THREE_MINUTES;
		} else if (timePeriodMinutes <= 360) {
			return CandlestickInterval.SIX_HOURLY;
		} else {
			throw new IllegalArgumentException("The time period is too large for analysis: " + timePeriodMinutes + " minutes");
		}
	}

	public long getServerTime() {
		return binanceApiRestClient.getServerTime();
	}

	public String getFuturesServerTime() {
		return futuresClient.market().time();
	}
}

