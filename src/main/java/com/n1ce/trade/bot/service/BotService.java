package com.n1ce.trade.bot.service;

import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.repositories.BotRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BotService extends AbstractService<Bot> {
	public BotService(BotRepository repository) {
		super(repository);
	}

	public List<Bot> getAllActiveBots() {
		return ((BotRepository) repository).findAllByIsRunning(true);
	}
}
