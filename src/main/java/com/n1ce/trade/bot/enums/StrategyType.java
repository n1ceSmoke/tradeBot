package com.n1ce.trade.bot.enums;

import com.n1ce.trade.bot.model.Strategy;

public enum StrategyType {
	LONG, SHORT;

	public static StrategyType of(String strategy) {
		if(strategy.equals(Strategy.LONG)) {
			return LONG;
		}
		return SHORT;
	}
}
