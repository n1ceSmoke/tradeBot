package com.n1ce.trade.bot.scheduler;

import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.service.BotService;
import com.n1ce.trade.bot.service.TradeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BotScheduler {

	private final TradeService tradeService;
	private final BotService botService;

	public BotScheduler(TradeService tradeService, BotService botService) {
		this.tradeService = tradeService;
		this.botService = botService;
	}

	@Scheduled(fixedRate = 30000) // Запуск каждые 30 секунд
	public void manageTrades() {
		List<Bot> activeBots = botService.getAllActiveBots();
		for (Bot bot : activeBots) {
			tradeService.executeTradeLogic(bot);
		}
	}
}
