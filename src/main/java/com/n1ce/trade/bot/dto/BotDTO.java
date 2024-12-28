package com.n1ce.trade.bot.dto;

public class BotDTO implements AbstractDTO {
	private Long id;
	private String name;
	private String marketPair;
	private Boolean isRunning;
	private Double deposit;
	private Integer deadlineMinutes;
	private Long strategyID;
	private Long profitConfigID;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMarketPair() {
		return marketPair;
	}

	public void setMarketPair(String marketPair) {
		this.marketPair = marketPair;
	}

	public Boolean getRunning() {
		return isRunning;
	}

	public void setRunning(Boolean running) {
		isRunning = running;
	}

	public Double getDeposit() {
		return deposit;
	}

	public void setDeposit(Double deposit) {
		this.deposit = deposit;
	}

	public Integer getDeadlineMinutes() {
		return deadlineMinutes;
	}

	public void setDeadlineMinutes(Integer deadlineMinutes) {
		this.deadlineMinutes = deadlineMinutes;
	}

	public Long getStrategyID() {
		return strategyID;
	}

	public void setStrategyID(Long strategyID) {
		this.strategyID = strategyID;
	}

	public Long getProfitConfigID() {
		return profitConfigID;
	}

	public void setProfitConfigID(Long profitConfigID) {
		this.profitConfigID = profitConfigID;
	}
}
