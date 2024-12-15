package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.model.Trade;
import com.n1ce.trade.bot.service.TradeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
public class TradeController extends AbstractCrudController<Trade> {
	public TradeController(TradeService tradeService) {
		super(tradeService);
	}
}

