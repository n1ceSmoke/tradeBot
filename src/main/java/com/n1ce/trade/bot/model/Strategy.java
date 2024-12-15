package com.n1ce.trade.bot.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Strategy {
	public static String LONG = "LONG";
	public static String SHORT = "SHORT";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	public Strategy(String name, String description) {
		this.name = name;
		this.description = description;
	}
}
