package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.dto.OrderDTO;
import com.n1ce.trade.bot.mapper.OrderMapper;
import com.n1ce.trade.bot.model.Order;
import com.n1ce.trade.bot.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController extends AbstractCrudController<Order, OrderDTO> {
	private final OrderService orderService;

	@Autowired
	public OrderController(OrderService orderService, OrderMapper orderMapper) {
		super(orderService, orderMapper);
		this.orderService = orderService;
	}

	@GetMapping("/by-bot/{botId}")
	public List<OrderDTO> getTradeHistoryByBotId(@PathVariable Long botId) {
		return orderService.getByBotID(botId);
	}
}

