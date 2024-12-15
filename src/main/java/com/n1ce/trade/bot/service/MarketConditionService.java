package com.n1ce.trade.bot.service;

import com.n1ce.trade.bot.model.MarketCondition;
import com.n1ce.trade.bot.repositories.MarketConditionRepository;
import org.springframework.stereotype.Service;

@Service
public class MarketConditionService extends AbstractService<MarketCondition> {
	public MarketConditionService(MarketConditionRepository repository) {
		super(repository);
	}
}
