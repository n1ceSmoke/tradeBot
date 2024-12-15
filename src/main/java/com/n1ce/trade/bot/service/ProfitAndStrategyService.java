package com.n1ce.trade.bot.service;

import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.model.MarketCondition;
import com.n1ce.trade.bot.model.Strategy;
import com.n1ce.trade.bot.repositories.RSIIndicatorRepository;
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
	public Strategy longTermMarketAnalyzeForStrategy(int periodMinutes) {
		LocalDateTime startTime = LocalDateTime.now().minusMinutes(periodMinutes);
		List<MarketCondition> marketConditions = marketConditionRepository.findByCreatedAtAfter(startTime);
		if (marketConditions.isEmpty()) {
			log.info("Недостаточно данных для анализа короткого периода");
		}
		double rsi = calculateLongTermRsi(marketConditions);
		if (rsi > 70) {
			return strategyRepository.findByName(Strategy.SHORT);
		} else if (rsi < 30) {
			return strategyRepository.findByName(Strategy.LONG);
		}

		double marketTrend = calculateMarketTrend(marketConditions);
		if (marketTrend > 0) {
			return strategyRepository.findByName(Strategy.LONG);
		} else {
			return strategyRepository.findByName(Strategy.SHORT);
		}
	}

	public double shortTermMarketAnalyzeForProfit(int periodMinutes, Bot bot) {
		LocalDateTime startTime = LocalDateTime.now().minusMinutes(periodMinutes);
		List<MarketCondition> marketConditions = marketConditionRepository.findByCreatedAtAfter(startTime);
		if (marketConditions.isEmpty()) {
			log.info("Недостаточно данных для анализа короткого периода");
		}

		double rsi = calculateShortTermRsi(marketConditions);
		double diffFromZero = Math.abs(rsi - 0);
		double diffFromHundred = Math.abs(100 - rsi);
		if (diffFromZero <= 30 || diffFromHundred <= 30) {
			return bot.getProfitConfig().getHighProfitThreshold();
		}

		double marketTrend = calculateMarketTrend(marketConditions);
		if (marketTrend > 1 || marketTrend < -1) {
			return bot.getProfitConfig().getHighProfitThreshold();
		}
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
}
