package com.n1ce.trade.bot.config;

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
		log.info("Запуск обновления индикаторов...");
		try {
			indicatorUpdateService.updateIndicators();
			log.info("Обновление индикаторов завершено успешно.");
		} catch (Exception e) {
			log.error("Ошибка при обновлении индикаторов: {}", e.getMessage());
		}
	}
}