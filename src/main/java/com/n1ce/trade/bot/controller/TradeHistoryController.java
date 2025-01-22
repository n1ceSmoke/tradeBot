package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.dto.TradeHistoryDTO;
import com.n1ce.trade.bot.mapper.TradeHistoryMapper;
import com.n1ce.trade.bot.model.TradeHistory;
import com.n1ce.trade.bot.service.TradeHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trade-history")
public class TradeHistoryController extends AbstractCrudController<TradeHistory, TradeHistoryDTO> {
	private final TradeHistoryService tradeHistoryService;

	@Autowired
	public TradeHistoryController(TradeHistoryService tradeHistoryService, TradeHistoryMapper mapper) {
		super(tradeHistoryService, mapper);
		this.tradeHistoryService = tradeHistoryService;
	}

	@GetMapping("/by-bot/{botId}")
	public List<TradeHistoryDTO> getTradeHistoryByBotId(@PathVariable Long botId) {
		return tradeHistoryService.getTradeHistoryByBotId(botId);
	}
}
