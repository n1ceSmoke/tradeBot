package com.n1ce.trade.bot.service;

import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.n1ce.trade.bot.model.FibonacciLevels;
import com.n1ce.trade.bot.model.MACD;
import com.n1ce.trade.bot.model.OrderBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IndicatorService {

	private final BinanceApiService binanceApiService;

	public IndicatorService(BinanceApiService binanceApiService) {
		this.binanceApiService = binanceApiService;
	}


	public OrderBlock detectOrderBlock(List<Candlestick> candles) {
		Candlestick maxVolumeCandle = candles.stream()
				.max(Comparator.comparingDouble(c -> Double.parseDouble(c.getVolume())))
				.orElse(null);

		if (maxVolumeCandle == null) return null;

		return new OrderBlock(
				Double.parseDouble(maxVolumeCandle.getHigh()),
				Double.parseDouble(maxVolumeCandle.getLow())
		);
	}

	public double calculateRSI(String symbol, int period) {
		List<Double> prices = getClosingPrices(symbol, period + 1);
		if (prices.size() < period + 1) return 50.0; // нейтральное значение, если недостаточно данных

		double gainSum = 0, lossSum = 0;
		for (int i = 1; i < prices.size(); i++) {
			double diff = prices.get(i) - prices.get(i - 1);
			if (diff > 0) {
				gainSum += diff;
			} else {
				lossSum += Math.abs(diff);
			}
		}

		double avgGain = gainSum / period;
		double avgLoss = lossSum / period;

		if (avgLoss == 0) return 100.0;

		double rs = avgGain / avgLoss;
		return 100 - (100 / (1 + rs));
	}

	public MACD calculateMACD(String symbol, int fast, int slow, int signal) {
		int requiredCandles = slow + signal;
		List<Double> prices = getClosingPrices(symbol, requiredCandles);
		if (prices.size() < requiredCandles) return new MACD(0, 0, false);

		List<Double> emaFastSeries = calculateEMASeries(prices, fast);
		List<Double> emaSlowSeries = calculateEMASeries(prices, slow);

		List<Double> macdSeries = new ArrayList<>();
		int minSize = Math.min(emaFastSeries.size(), emaSlowSeries.size());
		for (int i = 0; i < minSize; i++) {
			macdSeries.add(emaFastSeries.get(i) - emaSlowSeries.get(i));
		}

		double macdValue = macdSeries.get(macdSeries.size() - 1);
		double signalLine = calculateEMA(macdSeries, signal);

		boolean isBearish = macdValue < signalLine;
		return new MACD(macdValue, signalLine, isBearish);
	}

	public FibonacciLevels calculateFibonacci(double high, double low) {
		return new FibonacciLevels(
				low + (high - low) * 0.618,
				low + (high - low) * 0.786
		);
	}

	public boolean detectBearishEngulfing(List<Candlestick> candles) {
		if (candles.size() < 2) return false;

		Candlestick prev = candles.get(candles.size() - 2);
		Candlestick current = candles.get(candles.size() - 1);

		boolean isBearish = Double.parseDouble(current.getClose()) < Double.parseDouble(current.getOpen());
		boolean prevBullish = Double.parseDouble(prev.getClose()) > Double.parseDouble(prev.getOpen());

		boolean engulfs = Double.parseDouble(current.getOpen()) > Double.parseDouble(prev.getClose()) &&
				Double.parseDouble(current.getClose()) < Double.parseDouble(prev.getOpen());

		return isBearish && prevBullish && engulfs;
	}

	public boolean detectPinBar(List<Candlestick> candles) {
		if (candles.isEmpty()) return false;

		Candlestick candle = candles.get(candles.size() - 1);

		double open = Double.parseDouble(candle.getOpen());
		double close = Double.parseDouble(candle.getClose());
		double high = Double.parseDouble(candle.getHigh());
		double low = Double.parseDouble(candle.getLow());

		double body = Math.abs(close - open);
		double upperShadow = high - Math.max(open, close);
		double lowerShadow = Math.min(open, close) - low;

		return (lowerShadow > body * 2 && upperShadow < body) ||
				(upperShadow > body * 2 && lowerShadow < body);
	}

	private List<Double> calculateEMASeries(List<Double> values, int period) {
		List<Double> emaSeries = new ArrayList<>();
		if (values.isEmpty()) return emaSeries;
		double multiplier = 2.0 / (period + 1);
		double ema = values.get(0);
		emaSeries.add(ema);
		for (int i = 1; i < values.size(); i++) {
			ema = (values.get(i) - ema) * multiplier + ema;
			emaSeries.add(ema);
		}
		return emaSeries;
	}

	private double calculateEMA(List<Double> values, int period) {
		double multiplier = 2.0 / (period + 1);
		double ema = values.get(0);
		for (int i = 1; i < values.size(); i++) {
			ema = (values.get(i) - ema) * multiplier + ema;
		}
		return ema;
	}

	private List<Double> getClosingPrices(String symbol, int count) {
		return binanceApiService.getCandlestickDataWithLimit(symbol, CandlestickInterval.FIVE_MINUTES, count).stream()
				.map(c -> Double.parseDouble(c.getClose()))
				.collect(Collectors.toList());
	}
}


