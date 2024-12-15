package com.n1ce.trade.bot.repositories;

import com.n1ce.trade.bot.model.RSIIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RSIIndicatorRepository extends JpaRepository<RSIIndicator, Long> {
	// Найти по боту
	List<RSIIndicator> findByBot_Id(Long botId);

	// Найти последние значения
	List<RSIIndicator> findTop10ByBot_IdOrderByCreatedAtDesc(Long botId);
	List<RSIIndicator> findByCreatedAtAfter(LocalDateTime timestamp);
	@Query("SELECT COALESCE(AVG(r.rsiValue), 50) FROM RSIIndicator r WHERE r.createdAt > :startTime")
	Double findAverageRsiAfter(@Param("startTime") LocalDateTime startTime);
}
