package com.n1ce.trade.bot.service;

import com.n1ce.trade.bot.model.ProfitConfig;
import com.n1ce.trade.bot.repositories.ProfitConfigRepository;
import org.springframework.stereotype.Service;

@Service
public class ProfitConfigService extends AbstractService<ProfitConfig> {
	public ProfitConfigService(ProfitConfigRepository repository) {
		super(repository);
	}
}
