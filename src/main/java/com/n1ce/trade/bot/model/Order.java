package com.n1ce.trade.bot.model;

import com.n1ce.trade.bot.enums.OrderType;
import com.n1ce.trade.bot.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String symbol;

	@ManyToOne
	@JoinColumn(name = "trade_id", nullable = false)
	private Trade trade;

	@ManyToOne
	@JoinColumn(name = "bot_id", nullable = false)
	private Bot bot;

	@Enumerated(EnumType.STRING)
	private OrderType type; // BUY or SELL

	private double quantity;

	private double price;

	@Enumerated(EnumType.STRING)
	private OrderStatus status; // PENDING, FILLED, CANCELLED

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	// Геттеры и сеттеры
}
