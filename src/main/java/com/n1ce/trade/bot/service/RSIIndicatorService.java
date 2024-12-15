package com.n1ce.trade.bot.service;

import com.n1ce.trade.bot.model.RSIIndicator;
import com.n1ce.trade.bot.repositories.RSIIndicatorRepository;
import org.springframework.stereotype.Service;

@Service
public class RSIIndicatorService extends AbstractService<RSIIndicator> {
	public RSIIndicatorService(RSIIndicatorRepository repository) {
		super(repository);
	}
}
