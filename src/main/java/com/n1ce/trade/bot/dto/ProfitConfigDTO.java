package com.n1ce.trade.bot.dto;

public class ProfitConfigDTO implements AbstractDTO {
	private Long id;

	private Double highProfit;

	private Double lowProfit;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Double getHighProfit() {
		return highProfit;
	}

	public void setHighProfit(Double highProfit) {
		this.highProfit = highProfit;
	}

	public Double getLowProfit() {
		return lowProfit;
	}

	public void setLowProfit(Double lowProfit) {
		this.lowProfit = lowProfit;
	}
}
