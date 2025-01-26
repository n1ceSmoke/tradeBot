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
 * WGPTradeService is responsible for managing trading logic and operations specific
 * to the WGP strategy. It provides methods to handle trade execution, creation,
 * and update processes. The service interacts with various repositories and
 * external services such as Binance API to perform trading-related operations.
 *
 * This class extends TradeService and delegates core trading functionalities
 * to its parent class while enhancing it with WGP-specific logic.
 */
@Log4j2
@Service("WGP")
public class WGPTradeService extends TradeService {
	private final ProfitAndStrategyService profitAndStrategyService;


	public WGPTradeService(TradeRepository repository,
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
		List<Order> orders = orderService.getByBotAndStatus(bot, OrderStatus.PENDING);

		if (orders.isEmpty()) {
			log.info("Creating first order in trade...");
			createFirstOrderInTrade(bot, trade);
			return;
		}
		if (checkIsOrderFilled(orders, bot, trade)) {
			return;
		}
		if (trade.getStatus().equals(TradeStatus.PENDING)) {
			executeTrailingStop(bot, trade, orders.get(0));
			return;
		}

		checkForProfitPullBack(bot, trade, orders.get(0));
		closeTradeIfStuck(bot, trade, orders.get(0));
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
		return tradeRepository.save(trade);
	}

	private void createFirstOrderInTrade(Bot bot, Trade trade) {
		Signal signal = profitAndStrategyService.longTermMarketAnalyzeForStrategy(360, bot);
		if (signal != null) {
			orderService.createOrder(bot, signal.getType(), false, trade);
		}
	}

	private void executeTrailingStop(Bot bot, Trade trade, Order order) {
		double currentPrice = binanceApiService.getCurrentPrice(bot.getMarketPair());
		if (currentPrice <= trade.getBuyPrice() || shouldAdjustBuyOrder(bot, order)) {
			if (binanceApiService.cancelOrder(order.getId(), order.getSymbol())) {
				log.info("Executing Trailing Stop action...");
				order.setStatus(OrderStatus.CANCELLED);
				orderService.save(order);
				Signal signal = profitAndStrategyService.longTermMarketAnalyzeForStrategy(360, bot);
				if (signal != null) {
					orderService.createOrder(bot, signal.getType(), false, trade);
				}
			}
		}
	}
}
