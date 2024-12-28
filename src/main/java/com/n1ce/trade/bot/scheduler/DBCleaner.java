package com.n1ce.trade.bot.scheduler;

import com.n1ce.trade.bot.service.MarketConditionService;
import com.n1ce.trade.bot.service.RSIIndicatorService;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Log4j2
@Component
public class DBCleaner {
	private final MarketConditionService marketConditionService;
	private final RSIIndicatorService rsiIndicatorService;

	public DBCleaner(MarketConditionService marketConditionService, RSIIndicatorService rsiIndicatorService) {
		this.marketConditionService = marketConditionService;
		this.rsiIndicatorService = rsiIndicatorService;
	}

	@Scheduled(fixedRate = 43200000, initialDelay = 10000)
	public void cleanOldData() {
		log.info("Start cleaning database...");
		LocalDateTime deleteFrom = LocalDateTime.now().minusHours(12);
		marketConditionService.findAll().stream().filter(mc -> mc.getCreatedAt().isBefore(deleteFrom)).forEach(mc -> marketConditionService.deleteById(mc.getId()));
		rsiIndicatorService.findAll().stream().filter(rsi -> rsi.getCreatedAt().isBefore(deleteFrom)).forEach(rsi -> rsiIndicatorService.deleteById(rsi.getId()));
		log.info("All old marketCondition and rsiIndicator entries are cleared");
	}
}
