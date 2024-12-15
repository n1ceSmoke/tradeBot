package com.n1ce.trade.bot.repositories;

import com.n1ce.trade.bot.enums.TradeStatus;
import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
	List<Trade> findByBot_Id(Long botId);
	List<Trade> findByStatus(TradeStatus status);

	boolean existsByBotAndStatus(Bot bot, TradeStatus status);
	Trade findByStatusAndBot(TradeStatus status, Bot bot);
}
