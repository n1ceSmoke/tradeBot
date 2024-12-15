package com.n1ce.trade.bot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RSIIndicator {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "bot_id", nullable = false)
	private Bot bot;

	@Column(nullable = false)
	private Integer timePeriod;

	@Column(nullable = false)
	private Double rsiValue;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
