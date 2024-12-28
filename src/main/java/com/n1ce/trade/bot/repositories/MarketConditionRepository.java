package com.n1ce.trade.bot.repositories;

import com.n1ce.trade.bot.model.MarketCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketConditionRepository extends JpaRepository<MarketCondition, Long> {
	MarketCondition findTopByOrderByCreatedAtDesc();
	List<MarketCondition> findByCreatedAtAfter(LocalDateTime timestamp);

	List<MarketCondition> findAllByCreatedAtAfterAndSymbol(LocalDateTime timestamp, String symbol);
}

