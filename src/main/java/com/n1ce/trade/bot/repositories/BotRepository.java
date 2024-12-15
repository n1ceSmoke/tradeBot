package com.n1ce.trade.bot.repositories;

import com.n1ce.trade.bot.model.Bot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BotRepository extends JpaRepository<Bot, Long> {
	List<Bot> findByNameContainingIgnoreCase(String name);

	List<Bot> findByMarketCondition_Id(Long marketConditionId);
	List<Bot> findAllByIsRunning(Boolean isRunning);
}
