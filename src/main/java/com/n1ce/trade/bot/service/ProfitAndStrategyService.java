package com.n1ce.trade.bot.service;

import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.n1ce.trade.bot.enums.StrategyType;
import com.n1ce.trade.bot.model.*;
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
	private final IndicatorService indicatorService;
	private final BinanceApiService binanceApiService;

	@Autowired
	public ProfitAndStrategyService(MarketConditionRepository marketConditionRepository, IndicatorService indicatorService, BinanceApiService binanceApiService) {
		this.marketConditionRepository = marketConditionRepository;
		this.indicatorService = indicatorService;
		this.binanceApiService = binanceApiService;
	}

	public Signal longTermMarketAnalyzeForStrategy(int periodMinutes, Bot bot) {
		LocalDateTime startTime = LocalDateTime.now().minusMinutes(periodMinutes);
		List<MarketCondition> marketConditions = marketConditionRepository.findAllByCreatedAtAfterAndSymbol(startTime, bot.getMarketPair());

		if (marketConditions.isEmpty()) {
			log.info("No market conditions available for long-term analysis.");
			return null;
		}

		double longTermRSI = calculateLongTermRsi(marketConditions);
		double marketTrend = calculateMarketTrend(marketConditions);
		boolean sufficientVolume = isVolumeSufficient(marketConditions, 1.2);
		double ema = calculateEMA(marketConditions, 2);
		double lastPrice = marketConditions.get(marketConditions.size() - 1).getPrice();

		double score = 0;
		score += (longTermRSI > 70 ? -1 : (longTermRSI < 30 ? 1 : 0)) * 0.5;
		score += (marketTrend > 0 ? -1 : 1) * 0.25;
		score += (sufficientVolume ? 0.15 : -0.15);
		score += (lastPrice > ema ? 0.1 : -0.1);

		StrategyType strategyType = score > 0 ? StrategyType.LONG : (score < 0 ? StrategyType.SHORT : null);
		return strategyType != null ? new Signal(strategyType, Math.abs(score)) : null;
	}

	public Signal shortTermMarketAnalyzeForStrategy(int periodMinutes, Bot bot) {
		LocalDateTime startTime = LocalDateTime.now().minusMinutes(periodMinutes);
		List<MarketCondition> marketConditions = marketConditionRepository.findAllByCreatedAtAfterAndSymbol(startTime, bot.getMarketPair());

		if (marketConditions.isEmpty()) {
			log.info("No market conditions available for short-term analysis.");
			return null;
		}

		double shortTermRSI = calculateShortTermRsi(marketConditions);
		double atr = calculateATR(marketConditions, 2);
		boolean sufficientVolume = isVolumeSufficient(marketConditions, 1.2);

		double score = 0;
		score += (shortTermRSI > 70 ? -1 : (shortTermRSI < 30 ? 1 : 0)) * 0.5;
		score += (atr > 1.0 ? 0.3 : -0.3);
		score += (sufficientVolume ? 0.2 : -0.2);

		StrategyType strategyType = score > 0 ? StrategyType.LONG : (score < 0 ? StrategyType.SHORT : null);
		return strategyType != null ? new Signal(strategyType, Math.abs(score)) : null;
	}

	public double shortTermMarketAnalyzeForProfit(int periodMinutes, Bot bot) {
		LocalDateTime startTime = LocalDateTime.now().minusMinutes(periodMinutes);
		List<MarketCondition> marketConditions = marketConditionRepository.findAllByCreatedAtAfterAndSymbol(startTime, bot.getMarketPair());

		if (marketConditions.isEmpty()) {
			log.info("No market conditions available for short-term analysis.");
			return bot.getProfitConfig().getLowProfit();
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
			return bot.getProfitConfig().getHighProfit();
		}

		log.info("Low profit threshold applied for bot: {}", bot.getId());
		return bot.getProfitConfig().getLowProfit();
	}

	public Signal analyzeMarket(Bot bot) {
		String symbol = bot.getMarketPair();

		OrderBlock orderBlock = detectOrderBlock(symbol);
		if (orderBlock == null) {
			log.info("No significant order block found for {}", symbol);
			return null;
		}

		if (!isCorrectionConfirmed(symbol)) {
			log.info("Correction not confirmed for {}", symbol);
			return null;
		}

		FibonacciLevels fibLevels = indicatorService.calculateFibonacci(orderBlock.getHigh(), orderBlock.getLow());

		Signal shortTermMarket = shortTermMarketAnalyzeForStrategy(5, bot);
		log.info("Correction not confirmed for {}", symbol);
		return combineSignals(shortTermMarket, orderBlock, fibLevels);
	}

	private OrderBlock detectOrderBlock(String symbol) {
		List<Candlestick> candles = binanceApiService.getCandlestickDataWithLimit(symbol, CandlestickInterval.FIFTEEN_MINUTES, 50);
		return indicatorService.detectOrderBlock(candles);
	}

	private boolean isCorrectionConfirmed(String symbol) {
		double rsi = indicatorService.calculateRSI(symbol, 14);
		MACD macd = indicatorService.calculateMACD(symbol, 12, 26, 9);
		return rsi < 70 && macd.isBearish();
	}

	private Signal combineSignals(Signal shortTermSignal, OrderBlock orderBlock, FibonacciLevels fibLevels) {
		double score = 0;
		score += (fibLevels.getLevel618() > (orderBlock.getHigh() + orderBlock.getLow()) / 2 ? 1 : -1) * 0.7;
		score += (shortTermSignal != null ? shortTermSignal.getStrength() * 0.6 : -0.4);
		StrategyType strategyType = score > 0 ? StrategyType.LONG : (score < 0 ? StrategyType.SHORT : null);

		return strategyType != null ? new Signal(strategyType, score) : null;
	}


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
			return 0;
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
		double ema = marketConditions.get(0).getPrice();

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
