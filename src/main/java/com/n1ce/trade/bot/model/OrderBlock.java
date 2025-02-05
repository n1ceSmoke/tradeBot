package com.n1ce.trade.bot.model;

public class OrderBlock {
	private final double high;
	private final double low;

	public OrderBlock(double high, double low) {
		this.high = high;
		this.low = low;
	}

	public double getHigh() {
		return high;
	}

	public double getLow() {
		return low;
	}
}
