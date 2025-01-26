package com.n1ce.trade.bot.repositories;

import com.n1ce.trade.bot.model.TradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long> {
	List<TradeHistory> findByBot_IdAndCreatedAtAfter(Long botId, LocalDateTime createdAtAfter);

	List<TradeHistory> findByTrade_Id(Long tradeId);
}
