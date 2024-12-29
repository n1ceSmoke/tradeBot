package com.n1ce.trade.bot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "trade_history")
public class TradeHistory {
	public static final String DESCRIPTION_TEMPLATE = "Trade completed: buy order price %s, sell order price %s, profit %s";
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "trade_id", nullable = false)
	private Trade trade;

	@ManyToOne
	@JoinColumn(name = "bot_id", nullable = false)
	private Bot bot;

	@Column(columnDefinition = "TEXT")
	private String details;

	@Column
	private Integer label;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}

