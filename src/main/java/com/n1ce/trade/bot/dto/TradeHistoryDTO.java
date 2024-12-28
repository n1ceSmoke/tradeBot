package com.n1ce.trade.bot.dto;


public class TradeHistoryDTO implements AbstractDTO {
	private Long id;
	private Long tradeID;
	private Long botID;
	private String details;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTradeID() {
		return tradeID;
	}

	public void setTradeID(Long tradeID) {
		this.tradeID = tradeID;
	}

	public Long getBotID() {
		return botID;
	}

	public void setBotID(Long botID) {
		this.botID = botID;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}
}
