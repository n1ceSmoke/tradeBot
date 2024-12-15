package com.n1ce.trade.bot.service;

import com.n1ce.trade.bot.model.Strategy;
import com.n1ce.trade.bot.repositories.StrategyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class StrategyService extends AbstractService<Strategy> {

	@Autowired
	public StrategyService(StrategyRepository repository) {
		super(repository);
	}
}
