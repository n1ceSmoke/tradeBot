package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.dto.TradeHistoryDTO;
import com.n1ce.trade.bot.mapper.TradeHistoryMapper;
import com.n1ce.trade.bot.model.TradeHistory;
import com.n1ce.trade.bot.service.TradeHistoryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trade-history")
public class TradeHistoryController extends AbstractCrudController<TradeHistory, TradeHistoryDTO> {
	public TradeHistoryController(TradeHistoryService tradeHistoryService, TradeHistoryMapper mapper) {
		super(tradeHistoryService, mapper);
	}
}
