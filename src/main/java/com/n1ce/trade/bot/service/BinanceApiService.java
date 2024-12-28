package com.n1ce.trade.bot.service;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.exception.BinanceApiException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
public class BinanceApiService {
	private final String BINANCE_API_BASE_URL = "https://api.binance.com/api/v3";

	private final BinanceApiRestClient binanceApiRestClient;
	private final RestTemplate restTemplate;


	public BinanceApiService(BinanceApiRestClient binanceApiRestClient) {
		this.binanceApiRestClient = binanceApiRestClient;
		this.restTemplate = new RestTemplate();
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

	public NewOrderResponse createOrder(String symbol, OrderSide side, String quantity, double price) {
		try {
			NewOrder order = new NewOrder(symbol, side, OrderType.LIMIT, TimeInForce.GTC, String.format(Locale.US, "%.8f", Double.parseDouble(quantity)));
			order.recvWindow(5000L);
			order.price(new BigDecimal(price).setScale(2, RoundingMode.DOWN).toPlainString());
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
}

