package com.n1ce.trade.bot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "market_condition")
public class MarketCondition {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Double longTermRsi;

	@Column(nullable = false)
	private Double shortTermRsi;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(nullable = false)
	private Double price;

	@Column
	private Double volume; // Новый параметр для объёма торгов

	@Column
	private Double high; // Высокая цена свечи

	@Column
	private Double low; // Низкая цена свечи
	@Column
	private String symbol; // Низкая цена свечи
}
