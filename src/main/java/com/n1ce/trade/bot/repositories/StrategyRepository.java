package com.n1ce.trade.bot.repositories;

import com.n1ce.trade.bot.model.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrategyRepository extends JpaRepository<Strategy, Long> {
	Strategy findByName(String name);
}
