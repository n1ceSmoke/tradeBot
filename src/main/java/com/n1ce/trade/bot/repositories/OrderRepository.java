package com.n1ce.trade.bot.repositories;

import com.n1ce.trade.bot.enums.OrderStatus;
import com.n1ce.trade.bot.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
	List<Order> findByTradeId(Long tradeId);
	List<Order> findByBotIdAndStatus(Long botId, OrderStatus status);
}
