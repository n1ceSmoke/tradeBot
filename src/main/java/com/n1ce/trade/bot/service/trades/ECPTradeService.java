package com.n1ce.trade.bot.service.trades;

import com.n1ce.trade.bot.enums.OrderStatus;
import com.n1ce.trade.bot.enums.OrderType;
import com.n1ce.trade.bot.enums.TradeStatus;
import com.n1ce.trade.bot.model.*;
import com.n1ce.trade.bot.repositories.TradeHistoryRepository;
import com.n1ce.trade.bot.repositories.TradeRepository;
import com.n1ce.trade.bot.service.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static com.n1ce.trade.bot.model.TradeHistory.DESCRIPTION_TEMPLATE;

/**
 * ECPTradeService is a specialized service implementation that extends TradeService.
 * This service is responsible for managing trade operations specifically for bots
 * operating under the ECP strategy. It integrates logic for creating, managing,
 * and analyzing trades, as well as implementing trailing stop functionality.
 * Responsibilities include:
 * - Executing the trade logic for bots.
 * - Managing the lifecycle of trades, including creation and status updates.
 * - Analyzing market signals and strategy updates for trades.
 * - Handling pending orders associated with trades, including trailing stops.
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
		if(trade.getTradingVector() == null) {
			return;
		}
		if (orders.isEmpty()) {
			log.info("Checking for signal for first order in trade");
			orderService.checkForMarketSignalForMarketOrder(bot, trade);
			return;
		}
		if (checkIsOrderFilled(orders, bot, trade)) {
			return;
		}
		closeTradeIfStuck(bot, trade, orders);
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

	@Override
	protected void executeOrderFilledActions(Bot bot, Trade trade, Order order) {
		completeTrade(order, trade, bot);
	}

	@Override
	protected boolean checkIsOrderFilled(List<Order> orders, Bot bot, Trade trade) {
		int ordersFilled = 0;
		for (Order order : orders) {
			if (binanceApiService.isMarketOrderFilled(order.getId(), order.getSymbol())) {
				ordersFilled++;
				order.setStatus(OrderStatus.FILLED);
			}
		}
		if(ordersFilled >= 2) {
			log.info("Order is filled. Executing order close action...");
			orders.sort(Comparator.comparingDouble(Order::getPrice));
			Order marketOrder = orders.get(1);
			orders.stream().filter(o -> !o.getId().equals(marketOrder.getId()) || o.getStatus().equals(OrderStatus.FILLED)).forEach(orderService::save);
			executeOrderFilledActions(bot, trade, marketOrder);
			cancelAuxiliaryOrders(orders, bot);
			return true;
		}
		return false;
	}

	@Override
	protected void closeTradeIfStuck(Bot bot, Trade trade, Order order) {
		long hoursOpen = Duration.between(trade.getCreatedAt(), LocalDateTime.now()).toHours();
		double currentPrice = binanceApiService.getCurrentPrice(bot.getMarketPair());

		if (hoursOpen >= bot.getMaxTradeHours()) {
			log.info("Trade is closing due to timeframe. Closing price: {}, target price: {}", currentPrice, order.getPrice());
			binanceApiService.cancelFuturesOrder(order.getId(), order.getSymbol());
			order.setStatus(OrderStatus.CANCELLED);
			trade.setStatus(TradeStatus.CANCELED);
			tradeRepository.save(trade);
			orderService.save(order);
			orderService.createOrderWithPrice(bot, order.getType(), currentPrice, trade);
		}
	}

	@Override
	protected void completeTrade(Order order, Trade trade, Bot bot) {
		order.setStatus(OrderStatus.FILLED);
		orderService.save(order);
		if(order.getType().equals(OrderType.BUY)) trade.setBuyPrice(order.getPrice());
		else trade.setSellPrice(order.getPrice());
		trade.setStatus(TradeStatus.COMPLETED);
		List<Order> orders = orderService.getOrdersByTrade(trade);
		orders.sort(Comparator.comparingDouble(Order::getPrice));
		Order takeProfitOrder = orders.get(0);
		Order marketOrder = orders.get(1);
		Order stopLossOrder = orders.get(2);

		TradeHistory tradeHistory = new TradeHistory();
		tradeHistory.setTrade(trade);
		tradeHistory.setBot(bot);
		tradeHistory.setCreatedAt(LocalDateTime.now());
		BigDecimal entryPrice = BigDecimal.valueOf(marketOrder.getPrice());
		BigDecimal exitPrice = takeProfitOrder.getStatus().equals(OrderStatus.FILLED) ? BigDecimal.valueOf(takeProfitOrder.getPrice()) : BigDecimal.valueOf(stopLossOrder.getPrice());
		BigDecimal quantity = BigDecimal.valueOf(marketOrder.getQuantity());
		BigDecimal deposit = BigDecimal.valueOf(marketOrder.getQuantity()).multiply(BigDecimal.valueOf(marketOrder.getPrice()));
		tradeHistory.setDetails(calculateProfitAndBuildDescription(entryPrice, exitPrice, quantity, deposit));
		tradeHistory.setLabel(entryPrice.subtract(exitPrice).doubleValue() > 0 ? 1 : 0);


		tradeHistoryRepository.save(tradeHistory);
		tradeRepository.save(trade);
		log.info("Trade completed. {}", tradeHistory.getDetails());
		if(takeProfitOrder.getStatus().equals(OrderStatus.FILLED)) {
			stopLossOrder.setStatus(OrderStatus.CANCELLED);
			orderService.save(stopLossOrder);
		} else {
			takeProfitOrder.setStatus(OrderStatus.CANCELLED);
			orderService.save(stopLossOrder);
		}
	}

	private String calculateProfitAndBuildDescription(BigDecimal entryPrice, BigDecimal exitPrice, BigDecimal quantity, BigDecimal deposit) {
		BigDecimal pnl = exitPrice.subtract(entryPrice).multiply(quantity);
		BigDecimal roi = pnl.divide(deposit, MathContext.DECIMAL128).multiply(new BigDecimal("100"));
		return String.format("PnL: %s, ROi: %s", pnl, roi);
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

	private void cancelAuxiliaryOrders(List<Order> orders, Bot bot) {
		orders.forEach(order -> binanceApiService.cancelFuturesOrder(order.getId(), bot.getMarketPair()));
	}

	private void closeTradeIfStuck(Bot bot, Trade trade, List<Order> orders) {
		orders.forEach(o -> closeTradeIfStuck(bot, trade, o));
	}
}
