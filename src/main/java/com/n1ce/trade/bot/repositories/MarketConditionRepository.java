package com.n1ce.trade.bot.repositories;

import com.n1ce.trade.bot.model.MarketCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketConditionRepository extends JpaRepository<MarketCondition, Long> {
	MarketCondition findTopByOrderByCreatedAtDesc();
	List<MarketCondition> findByCreatedAtAfter(LocalDateTime timestamp);
}

