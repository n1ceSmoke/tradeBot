package com.n1ce.trade.bot.model;

import com.n1ce.trade.bot.enums.StrategyType;
import com.n1ce.trade.bot.enums.TradeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "bot_id")
	private Bot bot;

	@Enumerated(EnumType.STRING)
	private TradeStatus status; // CREATED, HALF_COMPLETED, COMPLETED

	private double buyPrice;
	private double sellPrice;

	@Enumerated(EnumType.STRING)
	private StrategyType orderType; // LONG or SHORT

	private LocalDateTime createdAt = LocalDateTime.now();
	private LocalDateTime updatedAt;


	private LocalDateTime completedAt;
}

