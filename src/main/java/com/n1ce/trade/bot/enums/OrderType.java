package com.n1ce.trade.bot.enums;

public enum OrderType {
	BUY, SELL;

	public static OrderType opposite(OrderType orderType) {
		if(orderType.equals(BUY)) {
			return SELL;
		}
		return BUY;
	}
}
