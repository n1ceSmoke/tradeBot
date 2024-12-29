package com.n1ce.trade.bot.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bot implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 50)
	private String marketPair;

	@Column(nullable = false, length = 50)
	private Boolean isRunning;

	@Column(nullable = false, length = 50)
	private Boolean isReinvest;

	@Column(nullable = false, length = 50)
	private Double deposit;

	@Column(nullable = false, length = 50)
	private Integer deadlineMinutes;

	@ManyToOne
	@JoinColumn(name = "strategy_id", nullable = false)
	private Strategy strategy;

	@ManyToOne
	@JoinColumn(name = "profit_config_id", nullable = false)
	private ProfitConfig profitConfig;

	@ManyToOne
	@JoinColumn(name = "market_condition_id", nullable = false)
	private MarketCondition marketCondition;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();

	public Bot(Long id) {
		this.id = id;
	}
}
