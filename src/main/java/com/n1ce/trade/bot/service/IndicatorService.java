package com.n1ce.trade.bot.service;

import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.n1ce.trade.bot.model.FibonacciLevels;
import com.n1ce.trade.bot.model.MACD;
import com.n1ce.trade.bot.model.OrderBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IndicatorService {
	private static final int RSI_PERIOD = 14;
	private static final int MACD_FAST = 12;
	private static final int MACD_SLOW = 26;
	private static final int MACD_SIGNAL = 9;
	private final BinanceApiService binanceApiService;

	public IndicatorService(BinanceApiService binanceApiService) {
		this.binanceApiService = binanceApiService;
	}

	public OrderBlock detectOrderBlock(List<Candlestick> candles) {
		Candlestick maxVolumeCandle = candles.stream()
				.max(Comparator.comparingDouble(c -> Double.parseDouble(c.getVolume())))
				.orElse(null);

		if (maxVolumeCandle == null) return null;

		return new OrderBlock(Double.parseDouble(maxVolumeCandle.getHigh()), Double.parseDouble(maxVolumeCandle.getLow()));
	}

	public double calculateRSI(String symbol, int period) {
		List<Double> prices = getClosingPrices(symbol, period + 1);
		if (prices.size() < period + 1) return 50.0;

		double gainSum = 0, lossSum = 0;
		for (int i = 1; i <= period; i++) {
			double diff = prices.get(i) - prices.get(i - 1);
			if (diff > 0) gainSum += diff;
			else lossSum -= diff;
		}

		double avgGain = gainSum / period;
		double avgLoss = lossSum / period;
		double rs = avgGain / avgLoss;
		return 100 - (100 / (1 + rs));
	}

	public MACD calculateMACD(String symbol, int fast, int slow, int signal) {
		List<Double> prices = getClosingPrices(symbol, slow + signal);
		double emaFast = calculateEMA(prices, fast);
		double emaSlow = calculateEMA(prices, slow);
		double macd = emaFast - emaSlow;
		double signalLine = calculateEMA(prices, signal);
		return new MACD(macd, signalLine, macd < signalLine);
	}

	public FibonacciLevels calculateFibonacci(double high, double low) {
		return new FibonacciLevels(
				low + (high - low) * 0.618,
				low + (high - low) * 0.786
		);
	}

	public boolean detectReversalPattern(List<Candlestick> candles) {
		if (candles.size() < 2) return false;
		Candlestick last = candles.get(candles.size() - 1);
		Candlestick prev = candles.get(candles.size() - 2);
		return Double.parseDouble(last.getClose()) > Double.parseDouble(last.getOpen()) && Double.parseDouble(prev.getClose()) < Double.parseDouble(prev.getOpen());
	}

	private double calculateEMA(List<Double> prices, int period) {
		double multiplier = 2.0 / (period + 1);
		double ema = prices.get(0);
		for (int i = 1; i < prices.size(); i++) {
			ema = (prices.get(i) - ema) * multiplier + ema;
		}
		return ema;
	}

	private List<Double> getClosingPrices(String symbol, int count) {
		return binanceApiService.getCandlestickDataWithLimit(symbol, CandlestickInterval.FIFTEEN_MINUTES, count).stream()
				.map(c -> Double.parseDouble(c.getClose()))
				.collect(Collectors.toList());
	}
}


