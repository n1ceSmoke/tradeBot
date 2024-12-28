package com.n1ce.trade.bot.scheduler;

import com.n1ce.trade.bot.service.IndicatorUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class IndicatorScheduler {

	private final IndicatorUpdateService indicatorUpdateService;

	@Scheduled(fixedRateString = "${scheduler.update-interval}", initialDelay = 10000)
	public void updateIndicators() {
		log.info("Starting indicator update...");
		try {
			indicatorUpdateService.updateIndicators();
			log.info("Indicator update completed successfully.");
		} catch (Exception e) {
			log.error("Error updating indicators: {}", e.getMessage());
		}
	}
}