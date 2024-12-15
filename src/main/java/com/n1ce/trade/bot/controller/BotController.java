package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.service.BotService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bots")
public class BotController extends AbstractCrudController<Bot> {
	public BotController(BotService botService) {
		super(botService);
	}
}
