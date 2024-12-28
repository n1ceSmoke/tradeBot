package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.dto.OrderDTO;
import com.n1ce.trade.bot.mapper.OrderMapper;
import com.n1ce.trade.bot.model.Order;
import com.n1ce.trade.bot.service.OrderService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController extends AbstractCrudController<Order, OrderDTO> {
	public OrderController(OrderService orderService, OrderMapper orderMapper) {
		super(orderService, orderMapper);
	}
}

