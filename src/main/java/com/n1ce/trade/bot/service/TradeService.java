package com.n1ce.trade.bot.service;

import com.n1ce.trade.bot.enums.OrderStatus;
import com.n1ce.trade.bot.enums.OrderType;
import com.n1ce.trade.bot.enums.StrategyType;
import com.n1ce.trade.bot.enums.TradeStatus;
import com.n1ce.trade.bot.model.*;
import com.n1ce.trade.bot.repositories.TradeHistoryRepository;
import com.n1ce.trade.bot.repositories.TradeRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.n1ce.trade.bot.model.TradeHistory.DESCRIPTION_TEMPLATE;

@Log4j2
@Service
public class TradeService extends AbstractService<Trade> {

	private final TradeRepository tradeRepository;
	private final TradeHistoryRepository tradeHistoryRepository;
	private final BinanceApiService binanceApiService;
	private final ProfitAndStrategyService profitAndStrategyService;
	private final OrderService orderService;
	private final BotService botService;


	public TradeService(TradeRepository repository,
						TradeRepository tradeRepository,
						TradeHistoryRepository tradeHistoryRepository,
						BinanceApiService binanceApiService,
						ProfitAndStrategyService profitAndStrategyService,
						OrderService orderService,
						BotService botService) {
		super(repository);
		this.tradeRepository = tradeRepository;
		this.tradeHistoryRepository = tradeHistoryRepository;
		this.binanceApiService = binanceApiService;
		this.profitAndStrategyService = profitAndStrategyService;
		this.orderService = orderService;
		this.botService = botService;
	}

	public void executeTradeLogic(Bot bot) {
		Trade trade = findActiveOrCreateNewTrade(bot);

		List<Order> orders = orderService.getByBotAndStatus(bot, OrderStatus.PENDING);
		if (orders.isEmpty()) {
			createFirstOrderInTrade(bot);
			return;
		}

		Order order = orders.get(0);
		if (binanceApiService.isOrderFilled(order.getId(), order.getSymbol())) {
			executeOrderFilledActions(bot, trade, order);
			return;
		}

		if (trade.getStatus().equals(TradeStatus.PENDING)) {
			executeTrailingStop(bot, trade, order);
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
		return tradeRepository.save(trade);
	}

	private void createFirstOrderInTrade(Bot bot) {
		Strategy strategy = profitAndStrategyService.longTermMarketAnalyzeForStrategy(360, bot);
		if (strategy != null) {
			orderService.createOrder(bot, strategy, false);
		}
	}

	private void executeOrderFilledActions(Bot bot, Trade trade, Order order) {
		if (trade.getStatus().equals(TradeStatus.PENDING)) {
			createSecondOrderOnTrade(bot, trade, order);
		} else if (trade.getStatus().equals(TradeStatus.SECOND_ORDER)) {
			completeTrade(order, trade, bot);
		}
	}

	private void createSecondOrderOnTrade(Bot bot, Trade trade, Order order) {
		Order newOrder = orderService.createOrder(bot, OrderType.opposite(order.getType()), true);
		if(null != newOrder) {
			order.setStatus(OrderStatus.FILLED);
			trade.setStatus(TradeStatus.SECOND_ORDER);
			if(order.getType().equals(OrderType.BUY)) trade.setBuyPrice(order.getPrice());
			else trade.setSellPrice(order.getPrice());

			tradeRepository.save(trade);
			orderService.save(order);
		}
	}


	private void executeTrailingStop(Bot bot, Trade trade, Order order) {
		double currentPrice = binanceApiService.getCurrentPrice(bot.getMarketPair());
		if (currentPrice <= trade.getBuyPrice() || shouldAdjustBuyOrder(bot, order)) {
			if (binanceApiService.cancelOrder(order.getId(), order.getSymbol())) {
				order.setStatus(OrderStatus.CANCELLED);
				orderService.save(order);
				Strategy strategy = profitAndStrategyService.longTermMarketAnalyzeForStrategy(360, bot);
				if (strategy != null) {
					orderService.createOrder(bot, strategy, false);
				}
			}
		}
	}
	private void completeTrade(Order order, Trade trade, Bot bot) {
		order.setStatus(OrderStatus.FILLED);
		if(order.getType().equals(OrderType.BUY)) trade.setBuyPrice(order.getPrice());
		else trade.setSellPrice(order.getPrice());
		trade.setStatus(TradeStatus.COMPLETED);

		double profit = (trade.getSellPrice() - trade.getBuyPrice()) * order.getQuantity();
		double toReinvest = profit / 2;
		bot.setDeposit(bot.getDeposit() + toReinvest);

		TradeHistory tradeHistory = new TradeHistory();
		tradeHistory.setTrade(trade);
		tradeHistory.setBot(bot);
		tradeHistory.setDetails(String.format(DESCRIPTION_TEMPLATE, trade.getBuyPrice(), trade.getSellPrice()));
		tradeHistory.setCreatedAt(LocalDateTime.now());
		tradeHistory.setLabel(profit > 0 ? 1 : 0);

		tradeHistoryRepository.save(tradeHistory);
		orderService.save(order);
		tradeRepository.save(trade);
		botService.save(bot);
		log.info("Trade completed. Profit {} USDT", profit);
	}

	private boolean shouldAdjustBuyOrder(Bot bot, Order order) {
		// Логика для подтяжки цены, если еще не выполнен первый ордер
		LocalDateTime adjustmentDeadline = order.getCreatedAt().plusMinutes(bot.getDeadlineMinutes());
		return adjustmentDeadline.isBefore(LocalDateTime.now());
	}
}
