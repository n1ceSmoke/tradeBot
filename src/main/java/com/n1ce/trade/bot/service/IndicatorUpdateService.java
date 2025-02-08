package com.n1ce.trade.bot.service;

import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.model.MarketCondition;
import com.n1ce.trade.bot.model.RSIIndicator;
import com.n1ce.trade.bot.repositories.BotRepository;
import com.n1ce.trade.bot.repositories.MarketConditionRepository;
import com.n1ce.trade.bot.repositories.RSIIndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class IndicatorUpdateService {

	private final BotRepository botRepository;
	private final RSIIndicatorRepository rsiIndicatorRepository;
	private final MarketConditionRepository marketConditionRepository;
	private final BinanceApiService binanceApiService;

	public void updateIndicators() {
		List<Bot> bots = botRepository.findAll();

		if (bots.isEmpty()) {
			log.info("No active bots for indicator updates");
		}

		for (Bot bot : bots) {
			try {
				updateRSIIndicator(bot);
				updateMarketCondition(bot);
			} catch (Exception e) {
				log.error("Error updating indicators for bot: {}. Reason: {}", bot.getId(), e.getMessage());
			}
		}
	}

	private void updateRSIIndicator(Bot bot) {
		// Получаем данные по цене для расчета RSI
		List<Double> priceData = binanceApiService.getPriceHistory(bot.getMarketPair(), 360);

		if (priceData.isEmpty()) {
			log.info("Insufficient data to calculate RSI for bot: " + bot.getId());
		}

		// Расчет RSI
		double rsiValue = calculateRSI(priceData, 14);

		// Сохранение в таблицу RSIIndicator
		RSIIndicator rsiIndicator = new RSIIndicator();
		rsiIndicator.setBot(bot);
		rsiIndicator.setTimePeriod(360);
		rsiIndicator.setRsiValue(rsiValue);
		rsiIndicator.setCreatedAt(LocalDateTime.now());
		rsiIndicatorRepository.save(rsiIndicator);

		log.info("RSI updated for bot {}: {}", bot.getId(), rsiValue);
	}

	private void updateMarketCondition(Bot bot) {
		// Получаем данные свечей
		List<Candlestick> candlesticks = binanceApiService.getCandlestickData(bot.getMarketPair(), CandlestickInterval.ONE_MINUTE);
		if (candlesticks.isEmpty()) {
			log.info("No candlestick data available for bot: " + bot.getId());
			return;
		}

		Candlestick lastCandle = candlesticks.get(0);

		// Рассчитываем long-term и short-term RSI
		List<RSIIndicator> rsiIndicators = rsiIndicatorRepository.findByBot_Id(bot.getId());
		if (rsiIndicators.isEmpty()) {
			log.info("Нет данных RSI для анализа MarketCondition для бота: " + bot.getId());
		}
		double longTermRSI = rsiIndicators.stream()
				.mapToDouble(RSIIndicator::getRsiValue)
				.average()
				.orElse(50.0);

		double shortTermRSI = rsiIndicators.stream()
				.filter(rsi -> rsi.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5)))
				.mapToDouble(RSIIndicator::getRsiValue)
				.average()
				.orElse(50.0);

		// Создаём новую запись MarketCondition
		MarketCondition marketCondition = new MarketCondition();
		marketCondition.setLongTermRsi(longTermRSI);
		marketCondition.setShortTermRsi(shortTermRSI);
		marketCondition.setPrice(Double.parseDouble(lastCandle.getClose()));
		marketCondition.setVolume(Double.parseDouble(lastCandle.getVolume()));
		marketCondition.setHigh(Double.parseDouble(lastCandle.getHigh()));
		marketCondition.setLow(Double.parseDouble(lastCandle.getLow()));
		marketCondition.setCreatedAt(LocalDateTime.now());
		marketCondition.setSymbol(bot.getMarketPair());
		marketConditionRepository.save(marketCondition);

		bot.setMarketCondition(marketCondition);
		botRepository.save(bot);

		log.info("MarketCondition updated for bot {}: LongTermRSI={}, ShortTermRSI={}, Price={}, Volume={}, High={}, Low={}",
				bot.getId(), longTermRSI, shortTermRSI, marketCondition.getPrice(), marketCondition.getVolume(),
				marketCondition.getHigh(), marketCondition.getLow());
	}


	private double calculateRSI(List<Double> priceData, int period) {
		if (priceData.size() < period + 1) {
			log.info("Insufficient data to calculate RSI for bot.");
		}

		double gainSum = 0.0;
		double lossSum = 0.0;

		// Начальный расчет для первого периода
		for (int i = 1; i <= period; i++) {
			double difference = priceData.get(i) - priceData.get(i - 1);
			if (difference > 0) {
				gainSum += difference;
			} else {
				lossSum -= difference; // убыток делаем положительным
			}
		}

		double avgGain = gainSum / period;
		double avgLoss = lossSum / period;

		// Итеративный расчет для оставшихся точек (сглаживание)
		for (int i = period + 1; i < priceData.size(); i++) {
			double difference = priceData.get(i) - priceData.get(i - 1);
			if (difference > 0) {
				avgGain = (avgGain * (period - 1) + difference) / period;
				avgLoss = (avgLoss * (period - 1)) / period; // убытка нет
			} else {
				avgLoss = (avgLoss * (period - 1) - difference) / period;
				avgGain = (avgGain * (period - 1)) / period; // прироста нет
			}
		}

		// Избегаем деления на 0
		if (avgLoss == 0) {
			return 100.0;
		}

		double rs = avgGain / avgLoss;
		return 100.0 - (100.0 / (1.0 + rs));
	}

}
