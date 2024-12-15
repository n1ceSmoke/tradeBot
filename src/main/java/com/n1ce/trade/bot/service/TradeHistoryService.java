package com.n1ce.trade.bot.service;

import com.n1ce.trade.bot.model.TradeHistory;
import com.n1ce.trade.bot.repositories.TradeHistoryRepository;
import org.springframework.stereotype.Service;

@Service
public class TradeHistoryService extends AbstractService<TradeHistory> {
	public TradeHistoryService(TradeHistoryRepository repository) {
		super(repository);
	}
}
