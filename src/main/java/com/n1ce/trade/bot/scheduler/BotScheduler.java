package com.n1ce.trade.bot.scheduler;

import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.service.BotService;
import com.n1ce.trade.bot.service.TradeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BotScheduler {

	private final Map<String, TradeService> tradeServices;
	private final BotService botService;

	public BotScheduler(Map<String, TradeService> tradeServices, BotService botService) {
		this.tradeServices = tradeServices;
		this.botService = botService;
	}

	@Scheduled(fixedRate = 30000)
	public void manageTrades() {
		List<Bot> activeBots = botService.getAllActiveBots();
		for (Bot bot : activeBots) {
			TradeService tradeService = tradeServices.get(bot.getStrategy().getName());
			if(null != tradeService) {
				tradeService.executeTradeLogic(bot);
			} else {
				tradeServices.get("WGP").executeTradeLogic(bot);
			}
		}
	}
}
