package com.n1ce.trade.bot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "profit_config")
public class ProfitConfig {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Double highProfit;

	private Double lowProfit;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
