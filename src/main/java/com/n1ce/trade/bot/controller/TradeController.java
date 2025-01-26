package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.dto.TradeDTO;
import com.n1ce.trade.bot.mapper.TradeMapper;
import com.n1ce.trade.bot.model.Trade;
import com.n1ce.trade.bot.repositories.TradeRepository;
import com.n1ce.trade.bot.service.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/trades")
public class TradeController extends AbstractCrudController<Trade, TradeDTO> {
	private TradeServiceImpl service;

	public TradeController(TradeServiceImpl service, TradeMapper mapper) {
		super(service, mapper);
		this.service = service;
	}

	@GetMapping("/by-bot/{botId}")
	public List<TradeDTO> getTradesByBotId(@PathVariable Long botId) {
		return service.findByBotId(botId);
	}

}

@Service
class TradeServiceImpl extends AbstractService<Trade> {
	private final TradeMapper tradeMapper;

	@Autowired
	protected TradeServiceImpl(TradeRepository repository, TradeMapper tradeMapper) {
		super(repository);
		this.tradeMapper = tradeMapper;
	}

	public List<TradeDTO> findByBotId(Long id) {
		List<Trade> trades = ((TradeRepository) repository).findByBot_IdAndCreatedAtAfter(id, LocalDateTime.now().minusHours(24));
		return trades.stream().map(tradeMapper::toDto).collect(Collectors.toList());
	}
}
