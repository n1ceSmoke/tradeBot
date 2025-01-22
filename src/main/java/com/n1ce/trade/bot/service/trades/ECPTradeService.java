package com.n1ce.trade.bot.service.trades;

import com.n1ce.trade.bot.enums.OrderStatus;
import com.n1ce.trade.bot.enums.TradeStatus;
import com.n1ce.trade.bot.model.*;
import com.n1ce.trade.bot.repositories.TradeHistoryRepository;
import com.n1ce.trade.bot.repositories.TradeRepository;
import com.n1ce.trade.bot.service.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ECPTradeService is a specialized service implementation that extends TradeService.
 * This service is responsible for managing trade operations specifically for bots
 * operating under the ECP strategy. It integrates logic for creating, managing,
 * and analyzing trades, as well as implementing trailing stop functionality.
 *
 * Responsibilities include:
 * - Executing the trade logic for bots.
 * - Managing the lifecycle of trades, including creation and status updates.
 * - Analyzing market signals and strategy updates for trades.
 * - Handling pending orders associated with trades, including trailing stops.
 *
 * Dependencies injected into this service:
 * - TradeRepository: Handles database operations for trades.
 * - TradeHistoryRepository: Stores historical trade data.
 * - BinanceApiService: Interacts with Binance for market data and order management.
 * - ProfitAndStrategyService: Provides market analysis and strategy logic.
 * - OrderService: Manages order-related operations.
 * - BotService: Provides support for bot-related operations.
 */
@Log4j2
@Service("ECP")
public class ECPTradeService extends AbstractCurrentPriceTradeStrategy {
	private final ProfitAndStrategyService profitAndStrategyService;


	public ECPTradeService(TradeRepository repository,
						TradeRepository tradeRepository,
						TradeHistoryRepository tradeHistoryRepository,
						BinanceApiService binanceApiService,
						ProfitAndStrategyService profitAndStrategyService,
						OrderService orderService,
						BotService botService) {
		super(repository, tradeRepository, orderService,botService, tradeHistoryRepository, binanceApiService);
		this.profitAndStrategyService = profitAndStrategyService;
	}

	public void executeTradeLogic(Bot bot) {
		Trade trade = findActiveOrCreateNewTrade(bot);
		checkAndUpdateStrategy(trade, bot);
		List<Order> orders = orderService.getByBotAndStatus(bot, OrderStatus.PENDING);

		if (orders.isEmpty() && trade.getTradingVector() != null) {
			log.info("Checking for signal for first order in trade");
			orderService.checkForMarketSignalForOrder(bot, trade);
			return;
		}
		if(!orders.isEmpty()) {
			if (checkIsOrderFilled(orders, bot, trade)) {
				return;
			}
			if (trade.getStatus().equals(TradeStatus.PENDING)) {
				executeTrailingStop(bot, trade, orders.get(0));
				return;
			}
		}

		checkForProfitPullBack(bot, trade, orders.get(0));
		closeTradeIfStuck(bot, trade, orders.get(0));
	}

	private void checkAndUpdateStrategy(Trade trade, Bot bot) {
		if (trade.getTradingVector() == null) {
			Signal longTermSignal = profitAndStrategyService.longTermMarketAnalyzeForStrategy(120, bot);
			if (longTermSignal != null) {
				trade.setTradingVector(longTermSignal.getType());
				trade.setCreatedAt(LocalDateTime.now());
				tradeRepository.save(trade);
			}
		}
	}

	public Trade findActiveOrCreateNewTrade(Bot bot) {
		Trade trade = tradeRepository.findByStatusAndBot(TradeStatus.PENDING, bot);
		if (trade == null) {
			trade = tradeRepository.findByStatusAndBot(TradeStatus.SECOND_ORDER, bot);
		}
		if (trade == null) {
			trade = createNewTrade(bot);
		}
		return trade;
	}


	public Trade createNewTrade(Bot bot) {
		Trade trade = new Trade();
		trade.setBot(bot);
		trade.setStatus(TradeStatus.PENDING);
		trade.setCreatedAt(LocalDateTime.now());
		return trade;
	}
}
