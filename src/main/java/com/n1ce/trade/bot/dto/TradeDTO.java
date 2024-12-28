package com.n1ce.trade.bot.dto;

import com.n1ce.trade.bot.enums.TradeStatus;

public class TradeDTO implements AbstractDTO {
	private Long id;
	private Long botID;
	private TradeStatus status;
	private double buyPrice;
	private double sellPrice;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getBotID() {
		return botID;
	}

	public void setBotID(Long botID) {
		this.botID = botID;
	}

	public TradeStatus getStatus() {
		return status;
	}

	public void setStatus(TradeStatus status) {
		this.status = status;
	}

	public double getBuyPrice() {
		return buyPrice;
	}

	public void setBuyPrice(double buyPrice) {
		this.buyPrice = buyPrice;
	}

	public double getSellPrice() {
		return sellPrice;
	}

	public void setSellPrice(double sellPrice) {
		this.sellPrice = sellPrice;
	}
}
