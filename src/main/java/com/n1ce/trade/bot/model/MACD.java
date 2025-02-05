package com.n1ce.trade.bot.model;

public class MACD {
	private final double macdValue;
	private final double signalLine;
	private final boolean bearish;

	public MACD(double macdValue, double signalLine, boolean bearish) {
		this.macdValue = macdValue;
		this.signalLine = signalLine;
		this.bearish = bearish;
	}

	public double getMacdValue() {
		return macdValue;
	}

	public double getSignalLine() {
		return signalLine;
	}

	public boolean isBearish() {
		return bearish;
	}
}