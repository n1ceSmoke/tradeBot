package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.model.Order;
import com.n1ce.trade.bot.service.OrderService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController extends AbstractCrudController<Order> {
	public OrderController(OrderService orderService) {
		super(orderService);
	}
}

