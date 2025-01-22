package com.n1ce.trade.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.n1ce.trade.bot.enums.StrategyType;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Signal {
	private StrategyType type;
	private double strength;
	private LocalDateTime timestamp;


	public Signal(StrategyType type, double strength) {
		this.type = type;
		this.strength = strength;
		this.timestamp = LocalDateTime.now();
	}
}
