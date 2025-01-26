package com.n1ce.trade.bot.service;

import com.n1ce.trade.bot.dto.TradeHistoryDTO;
import com.n1ce.trade.bot.mapper.TradeHistoryMapper;
import com.n1ce.trade.bot.model.TradeHistory;
import com.n1ce.trade.bot.repositories.TradeHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TradeHistoryService extends AbstractService<TradeHistory> {
	private final TradeHistoryRepository tradeHistoryRepository;
	private final TradeHistoryMapper tradeHistoryMapper;
	public TradeHistoryService(TradeHistoryRepository repository, TradeHistoryMapper tradeHistoryMapper) {
		super(repository);
		this.tradeHistoryRepository = repository;
		this.tradeHistoryMapper = tradeHistoryMapper;
	}

	public List<TradeHistoryDTO> getTradeHistoryByBotId(Long botId) {
		List<TradeHistory> trades = ((TradeHistoryRepository) repository).findByBot_IdAndCreatedAtAfter(botId, LocalDateTime.now().minusHours(24));
		return trades.stream().map(tradeHistoryMapper::toDto).collect(Collectors.toList());
	}
}
