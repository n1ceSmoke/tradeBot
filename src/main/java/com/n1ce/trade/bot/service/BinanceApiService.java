package com.n1ce.trade.bot.service;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.CancelOrderResponse;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.exception.BinanceApiException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
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

	/**
	 * Получает данные о ценах для указанной торговой пары за определенный период.
	 *
	 * @param symbol Торговая пара, например, "BTCUSDT".
	 * @param periodMinutes Количество минут, за которые нужно получить данные.
	 * @return Список цен закрытия (close prices) за указанный период.
	 */
	public List<Double> getPriceData(String symbol, int periodMinutes) {
		// Определяем начальное время
		long startTime = Instant.now().minusSeconds(periodMinutes * 60L).toEpochMilli();

		// Получаем данные свечей (candlesticks) с Binance API
		List<Candlestick> candlesticks = binanceApiRestClient.getCandlestickBars(
				symbol,
				CandlestickInterval.ONE_MINUTE, // Интервал свечей (можно изменить, если нужно)
				null, // Количество свечей (null = все доступные)
				startTime,
				null
		);

		// Извлекаем цены закрытия из свечей
		return candlesticks.stream()
				.map(c -> Double.parseDouble(c.getClose()))
				.collect(Collectors.toList());
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
					log.info("Не удалось получить текущую цену из ответа Binance API");
					throw new RuntimeException("Ошибка при запросе текущей цены из Binance API: " + response.getStatusCode());
				}
			} else {
				log.info("Ошибка при запросе текущей цены из Binance API: " + response.getStatusCode());
				throw new RuntimeException("Ошибка при запросе текущей цены из Binance API: " + response.getStatusCode());
			}
		} catch (Exception e) {
			throw new RuntimeException("Ошибка при вызове Binance API: " + e.getMessage(), e);
		}
	}

	public NewOrderResponse createOrder(String symbol, OrderSide side, String quantity, double price) {
		try {
			NewOrder order = new NewOrder(symbol, side, OrderType.LIMIT, TimeInForce.GTC, quantity);
			order.price(String.valueOf(price));
			// Создаем ордер через Binance API
			NewOrderResponse orderResponse = binanceApiRestClient.newOrder(order);

			// Логируем успешное создание ордера
			System.out.println("Ордер успешно создан: " + orderResponse);

			return orderResponse;

		} catch (BinanceApiException e) {
			log.info("Не удалось создать ордер: " + e.getMessage());
			throw new RuntimeException("Не удалось создать ордер: " + e.getMessage());
		}
	}

	public boolean isOrderFilled(Long orderId, String symbol) {
		try {
			// Получаем информацию об ордере с Binance
			Order order = binanceApiRestClient.getOrderStatus(new OrderStatusRequest(symbol, orderId));

			// Проверяем статус ордера
			return "FILLED".equalsIgnoreCase(order.getStatus().name());
		} catch (BinanceApiException e) {
			log.info("Ошибка при проверке статуса ордера: " + e.getMessage(), e);
			throw new RuntimeException("Ошибка при проверке статуса ордера: " + e.getMessage());
		}
	}

	public boolean cancelOrder(Long orderId, String symbol) {
		try {
			// Отменяем ордер
			binanceApiRestClient.cancelOrder(new CancelOrderRequest(symbol, orderId));
			return true;
		} catch (BinanceApiException e) {
			// Логируем ошибку и возвращаем false
			System.err.println("Ошибка при отмене ордера: " + e.getMessage());
			return false;
		}
	}

	public List<Double> getPriceHistory(String symbol, int timePeriodMinutes) {
		// Определяем интервал свечей
		CandlestickInterval interval = determineInterval(timePeriodMinutes);

		// Вычисляем начальное время для запроса
		long currentTimeMillis = System.currentTimeMillis();
		long startTimeMillis = currentTimeMillis - (timePeriodMinutes * 60 * 1000);

		// Запрашиваем данные свечей (Candlestick)
		List<Candlestick> candlesticks = binanceApiRestClient.getCandlestickBars(symbol, interval, null, startTimeMillis, currentTimeMillis);

		// Извлекаем данные цен закрытия из свечей
		return candlesticks.stream()
				.map(Candlestick::getClose)
				.map(Double::parseDouble)
				.collect(Collectors.toList());
	}

	private CandlestickInterval determineInterval(int timePeriodMinutes) {
		if (timePeriodMinutes <= 3) {
			return CandlestickInterval.THREE_MINUTES;
		} else if (timePeriodMinutes <= 360) {
			return CandlestickInterval.SIX_HOURLY; // Добавлено для 360 минут
		} else {
			throw new IllegalArgumentException("Период времени слишком велик для анализа: " + timePeriodMinutes + " минут");
		}
	}
}

