package com.n1ce.trade.bot.repositories;

import com.n1ce.trade.bot.model.ProfitConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfitConfigRepository extends JpaRepository<ProfitConfig, Long> {
	// Дополнительные методы при необходимости
}
