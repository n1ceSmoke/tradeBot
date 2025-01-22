package com.n1ce.trade.bot.service.trades;

import com.n1ce.trade.bot.enums.OrderStatus;
import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.model.Order;
import com.n1ce.trade.bot.model.Trade;
import com.n1ce.trade.bot.repositories.TradeHistoryRepository;
import com.n1ce.trade.bot.repositories.TradeRepository;
import com.n1ce.trade.bot.service.BinanceApiService;
import com.n1ce.trade.bot.service.BotService;
import com.n1ce.trade.bot.service.OrderService;
import com.n1ce.trade.bot.service.TradeService;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.repository.JpaRepository;

@Log4j2
public abstract class AbstractCurrentPriceTradeStrategy extends TradeService {

	protected AbstractCurrentPriceTradeStrategy(JpaRepository<Trade, Long> repository, TradeRepository tradeRepository, OrderService orderService, BotService botService, TradeHistoryRepository tradeHistoryRepository, BinanceApiService binanceApiService) {
		super(repository, tradeRepository, orderService, botService, tradeHistoryRepository, binanceApiService);
	}

	protected void executeTrailingStop(Bot bot, Trade trade, Order order) {
		double currentPrice = binanceApiService.getCurrentPrice(bot.getMarketPair());
		if (currentPrice <= trade.getBuyPrice() || shouldAdjustBuyOrder(bot, order)) {
			log.info("Executing Trailing Stop action...");
			if (binanceApiService.cancelOrder(order.getId(), order.getSymbol())) {
				order.setStatus(OrderStatus.CANCELLED);
				orderService.save(order);
				orderService.checkForMarketSignalForOrder(bot, trade);
			}
		}
	}
}
