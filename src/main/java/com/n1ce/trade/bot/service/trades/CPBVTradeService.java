package com.n1ce.trade.bot.service.trades;

import com.n1ce.trade.bot.enums.OrderStatus;
import com.n1ce.trade.bot.enums.StrategyType;
import com.n1ce.trade.bot.enums.TradeStatus;
import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.model.Order;
import com.n1ce.trade.bot.model.Signal;
import com.n1ce.trade.bot.model.Trade;
import com.n1ce.trade.bot.repositories.TradeHistoryRepository;
import com.n1ce.trade.bot.repositories.TradeRepository;
import com.n1ce.trade.bot.service.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CPBVTradeService is responsible for implementing trade logic specific
 * to the CPBV (Current Price Based Vector) trading strategy. It extends
 * the AbstractCurrentPriceTradeStrategy to leverage shared functionality
 * for current price-based trade handling and includes additional specific
 * implementations for managing the trading lifecycle.
 *
 * This service incorporates the following key components:
 *
 * - Handles the creation and retrieval of trades for a given bot.
 * - Executes market analysis to determine appropriate trading strategies
 *   using the ProfitAndStrategyService.
 * - Places buy orders when no pending orders exist.
 * - Handles pending buy orders by managing order states and initiating
 *   trailing stop actions.
 *
 * This trading service operates as part of a bot trading system, managing
 * trades, market signals, and order states to ensure proper implementation
 * of the CPBV strategy.
 *
 * Dependency injection is used for services and repositories required to
 * perform trade and order management tasks.
 */
@Service("CPBV")
@Log4j2
public class CPBVTradeService extends AbstractCurrentPriceTradeStrategy {

	private final ProfitAndStrategyService profitAndStrategyService;

	@Autowired
	protected CPBVTradeService(JpaRepository<Trade, Long> repository,
							   TradeRepository tradeRepository,
							   OrderService orderService,
							   BotService botService,
							   TradeHistoryRepository tradeHistoryRepository,
							   BinanceApiService binanceApiService,
							   ProfitAndStrategyService profitAndStrategyService) {
		super(repository, tradeRepository, orderService, botService, tradeHistoryRepository, binanceApiService);
		this.profitAndStrategyService = profitAndStrategyService;
	}

	@Override
	public void executeTradeLogic(Bot bot) {
		Trade trade = findActiveOrCreateNewTrade(bot);
		if (trade.getTradingVector() == null) {
			analyzeMarketAndTrade(bot, trade);
		}
		List<Order> pendingOrders = orderService.getByBotAndStatus(bot, OrderStatus.PENDING);

		if (pendingOrders.isEmpty() && trade.getStatus().equals(TradeStatus.PENDING)) {
			log.info("Creating first order in trade...");
			placeBuyOrder(bot, trade);
			return;
		}
		if (checkIsOrderFilled(pendingOrders, bot, trade)) {
			return;
		}
		if (trade.getStatus().equals(TradeStatus.PENDING)) {
			log.info("Executing trailing stop...");
			executeTrailingStop(bot, trade, pendingOrders.get(0));
			return;
		}
		checkForProfitPullBack(bot, trade, pendingOrders.get(0));
		closeTradeIfStuck(bot, trade, pendingOrders.get(0));
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
		trade.setTradingVector(StrategyType.LONG);
		return tradeRepository.save(trade);
	}

	private void analyzeMarketAndTrade(Bot bot, Trade trade) {
		Signal signal = profitAndStrategyService.longTermMarketAnalyzeForStrategy(60, bot);

		if (signal != null && signal.getType().equals(StrategyType.LONG)) {
			trade.setTradingVector(StrategyType.LONG);
			tradeRepository.save(trade);
		}
	}

	private void placeBuyOrder(Bot bot, Trade trade) {
		log.info("Placing buy order for bot: {}", bot.getId());
		orderService.placeOrderIsSignalAboveThan(0.6, bot, trade, false);
	}
}
