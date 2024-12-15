package com.n1ce.trade.bot.service;

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

	@Transactional
	public void updateIndicators() {
		List<Bot> bots = botRepository.findAll();

		if (bots.isEmpty()) {
			log.info("Нет активных ботов для обновления индикаторов");
		}

		for (Bot bot : bots) {
			try {
				updateRSIIndicator(bot);
				updateMarketCondition(bot);
			} catch (Exception e) {
				// Логирование ошибки для конкретного бота
				log.error("Ошибка при обновлении индикаторов для бота: {}. Причина: {}", bot.getId(), e.getMessage());
			}
		}
	}

	private void updateRSIIndicator(Bot bot) {
		// Получаем данные по цене для расчета RSI
		List<Double> priceData = binanceApiService.getPriceHistory(bot.getMarketPair(), 360);

		if (priceData.isEmpty()) {
			log.info("Недостаточно данных для расчета RSI для бота: " + bot.getId());
		}

		// Расчет RSI
		double rsiValue = calculateRSI(priceData);

		// Сохранение в таблицу RSIIndicator
		RSIIndicator rsiIndicator = new RSIIndicator();
		rsiIndicator.setBot(bot);
		rsiIndicator.setTimePeriod(360);
		rsiIndicator.setRsiValue(rsiValue);
		rsiIndicator.setCreatedAt(LocalDateTime.now());
		rsiIndicatorRepository.save(rsiIndicator);

		log.info("RSI обновлен для бота {}: {}", bot.getId(), rsiValue);
	}

	private void updateMarketCondition(Bot bot) {
		// Получаем RSI данные для анализа
		List<RSIIndicator> rsiIndicators = rsiIndicatorRepository.findByBot_Id(bot.getId());

		if (rsiIndicators.isEmpty()) {
			log.info("Нет данных RSI для анализа MarketCondition для бота: " + bot.getId());
		}

		// Рассчитываем средние RSI для long-term и short-term периодов
		double longTermRSI = rsiIndicators.stream()// Условие для long-term
				.mapToDouble(RSIIndicator::getRsiValue)
				.average()
				.orElse(50.0); // Значение по умолчанию

		double shortTermRSI = rsiIndicators.stream()
				.filter(rsi -> rsi.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5)))
				.mapToDouble(RSIIndicator::getRsiValue)
				.average()
				.orElse(50.0);

		// Получаем текущую цену
		double currentPrice = binanceApiService.getCurrentPrice(bot.getMarketPair());

		// Сохраняем данные в таблицу MarketCondition
		MarketCondition marketCondition = new MarketCondition();
		marketCondition.setLongTermRsi(longTermRSI);
		marketCondition.setShortTermRsi(shortTermRSI);
		marketCondition.setPrice(currentPrice);
		marketCondition.setCreatedAt(LocalDateTime.now());
		marketConditionRepository.save(marketCondition);

		log.info("MarketCondition обновлен для бота {}: LongTermRSI={}, ShortTermRSI={}, Price={}",
				bot.getId(), longTermRSI, shortTermRSI, currentPrice);
	}

	private double calculateRSI(List<Double> priceData) {
		double gain = 0.0;
		double loss = 0.0;

		for (int i = 1; i < priceData.size(); i++) {
			double difference = priceData.get(i) - priceData.get(i - 1);

			if (difference > 0) {
				gain += difference;
			} else {
				loss -= difference; // Loss is positive
			}
		}

		double avgGain = gain / priceData.size();
		double avgLoss = loss / priceData.size();

		if (avgLoss == 0) {
			return 100.0; // RSI = 100 при отсутствии убытков
		}

		double rs = avgGain / avgLoss;
		return 100.0 - (100.0 / (1.0 + rs));
	}
}
