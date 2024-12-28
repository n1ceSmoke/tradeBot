package com.n1ce.trade.bot.service;

import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.model.MarketCondition;
import com.n1ce.trade.bot.model.Strategy;
import com.n1ce.trade.bot.repositories.StrategyRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.n1ce.trade.bot.repositories.MarketConditionRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Log4j2
@Service
public class ProfitAndStrategyService {

	private final MarketConditionRepository marketConditionRepository;
	private final StrategyRepository strategyRepository;

	@Autowired
	public ProfitAndStrategyService(MarketConditionRepository marketConditionRepository,StrategyRepository strategyRepository) {
		this.marketConditionRepository = marketConditionRepository;
		this.strategyRepository = strategyRepository;
	}

	/**
	 * Анализирует рынок и RSI индикатор за указанный период времени и возвращает стратегию (long/short).
	 * @param periodMinutes - Время в минутах для анализа рынка.
	 * @return Strategy - стратегия long или short.
	 */
	public Strategy longTermMarketAnalyzeForStrategy(int periodMinutes, Bot bot) {
		LocalDateTime startTime = LocalDateTime.now().minusMinutes(periodMinutes);
		List<MarketCondition> marketConditions = marketConditionRepository.findAllByCreatedAtAfterAndSymbol(startTime, bot.getMarketPair());

		if (marketConditions.isEmpty()) {
			log.info("No market conditions available for long-term analysis.");
			return null;
		}

		double longTermRSI = calculateLongTermRsi(marketConditions);
		double marketTrend = calculateMarketTrend(marketConditions);
		boolean sufficientVolume = isVolumeSufficient(marketConditions, 1.2);
		double ema = calculateEMA(marketConditions, 14);
		double lastPrice = marketConditions.get(marketConditions.size() - 1).getPrice();

		double score = 0;
		score += (longTermRSI > 70 ? -1 : (longTermRSI < 30 ? 1 : 0)) * 0.5;
		score += (marketTrend > 0 ? -1 : 1) * 0.25;
		score += (sufficientVolume ? 0.15 : -0.15);
		score += (lastPrice > ema ? 0.1 : -0.1);

		if (score > 0) {
			return strategyRepository.findByName(Strategy.LONG);
		} else if (score < 0) {
			return strategyRepository.findByName(Strategy.SHORT);
		}

		log.info("No clear signal for long-term strategy. Defaulting to null.");
		return null;
	}



	public double shortTermMarketAnalyzeForProfit(int periodMinutes, Bot bot) {
		LocalDateTime startTime = LocalDateTime.now().minusMinutes(periodMinutes);
		List<MarketCondition> marketConditions = marketConditionRepository.findAllByCreatedAtAfterAndSymbol(startTime, bot.getMarketPair());

		if (marketConditions.isEmpty()) {
			log.info("No market conditions available for short-term analysis.");
			return bot.getProfitConfig().getLowProfitThreshold();
		}

		double shortTermRSI = calculateShortTermRsi(marketConditions);
		double atr = calculateATR(marketConditions, 14);
		boolean sufficientVolume = isVolumeSufficient(marketConditions, 1.2);

		double score = 0;
		score += (shortTermRSI <= 30 ? 1 : (shortTermRSI >= 70 ? -1 : 0)) * 0.5;
		score += (atr > 1.0 ? 0.3 : -0.3);
		score += (sufficientVolume ? 0.2 : -0.2);

		if (score > 0) {
			log.info("High profit threshold triggered for bot: {}", bot.getId());
			return bot.getProfitConfig().getHighProfitThreshold();
		}

		log.info("Low profit threshold applied for bot: {}", bot.getId());
		return bot.getProfitConfig().getLowProfitThreshold();
	}



	/**
	 * Рассчитывает общий тренд рынка на основе изменений цен.
	 * @param marketConditions - Список данных рынка.
	 * @return double - тренд (положительное значение - рост, отрицательное - падение).
	 */
	private double calculateLongTermRsi(List<MarketCondition> marketConditions) {
		marketConditions.sort(Comparator.comparing(MarketCondition::getCreatedAt));
		return marketConditions.stream()
				.mapToDouble(MarketCondition::getLongTermRsi)
				.average()
				.orElse(0);
	}

	private double calculateShortTermRsi(List<MarketCondition> marketConditions) {
		marketConditions.sort(Comparator.comparing(MarketCondition::getCreatedAt));
		return marketConditions.stream()
				.mapToDouble(MarketCondition::getShortTermRsi)
				.average()
				.orElse(0);
	}

	/**
	 * Рассчитывает общий тренд рынка на основе изменений цен.
	 * @param marketConditions - Список данных рынка.
	 * @return double - тренд (положительное значение - рост, отрицательное - падение).
	 */
	private double calculateMarketTrend(List<MarketCondition> marketConditions) {
		marketConditions.sort(Comparator.comparing(MarketCondition::getCreatedAt));
		double averagePrice = marketConditions.stream()
				.mapToDouble(MarketCondition::getPrice)
				.average()
				.orElse(0);
		double lastPrice = marketConditions.get(marketConditions.size() - 1).getPrice();
		return lastPrice - averagePrice;
	}

	private double calculateATR(List<MarketCondition> marketConditions, int period) {
		if (marketConditions.size() < period) {
			return 0; // Недостаточно данных
		}

		double atr = 0.0;
		for (int i = 1; i < period; i++) {
			double high = marketConditions.get(i).getHigh();
			double low = marketConditions.get(i).getLow();
			double prevClose = marketConditions.get(i - 1).getPrice();
			double tr = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
			atr += tr;
		}

		return atr / period;
	}

	private double calculateEMA(List<MarketCondition> marketConditions, int period) {
		marketConditions.sort(Comparator.comparing(MarketCondition::getCreatedAt));
		double multiplier = 2.0 / (period + 1);
		double ema = marketConditions.get(0).getPrice(); // Начальная точка EMA

		for (int i = 1; i < marketConditions.size(); i++) {
			double price = marketConditions.get(i).getPrice();
			ema = (price - ema) * multiplier + ema;
		}

		return ema;
	}

	private boolean isVolumeSufficient(List<MarketCondition> marketConditions, double threshold) {
		double averageVolume = marketConditions.stream()
				.mapToDouble(MarketCondition::getVolume)
				.average()
				.orElse(0);
		double lastVolume = marketConditions.get(marketConditions.size() - 1).getVolume();
		return lastVolume >= averageVolume * threshold;
	}

}
