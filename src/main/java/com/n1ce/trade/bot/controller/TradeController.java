package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.dto.TradeDTO;
import com.n1ce.trade.bot.mapper.TradeMapper;
import com.n1ce.trade.bot.model.Trade;
import com.n1ce.trade.bot.service.TradeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
public class TradeController extends AbstractCrudController<Trade, TradeDTO> {
	public TradeController(TradeService tradeService, TradeMapper mapper) {
		super(tradeService, mapper);
	}
}

