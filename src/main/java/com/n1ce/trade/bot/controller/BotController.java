package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.dto.BotDTO;
import com.n1ce.trade.bot.mapper.BotMapper;
import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.service.BotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bots")
public class BotController extends AbstractCrudController<Bot, BotDTO> {
	private final BotService botService;
	public BotController(BotService botService, BotMapper mapper) {
		super(botService, mapper);
		this.botService = botService;
	}

	@GetMapping("/close-current-cycle/{botId}")
	public void closeCurrentCycle(@PathVariable Long botId) {
		botService.closeActiveCycleForBot(botId);
	}
}
