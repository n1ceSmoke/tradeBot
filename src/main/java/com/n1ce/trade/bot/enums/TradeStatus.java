package com.n1ce.trade.bot.enums;

public enum TradeStatus {
	PENDING("ACTIVE"), SECOND_ORDER("SECOND_ORDER_PENDING"), COMPLETED("FILLED"), CANCELED("CANCELED");

	private String status;
	TradeStatus(String status) {
		this.status = status;
	}

	public String getStatus() {
		return status;
	}
}
