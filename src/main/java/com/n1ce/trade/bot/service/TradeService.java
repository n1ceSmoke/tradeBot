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


	public TradeService(TradeRepository repository,
						TradeRepository tradeRepository,
						TradeHistoryRepository tradeHistoryRepository,
						BinanceApiService binanceApiService,
						ProfitAndStrategyService profitAndStrategyService,
						OrderService orderService) {
		super(repository);
		this.tradeRepository = tradeRepository;
		this.tradeHistoryRepository = tradeHistoryRepository;
		this.binanceApiService = binanceApiService;
		this.profitAndStrategyService = profitAndStrategyService;
		this.orderService = orderService;
	}

	public void executeTradeLogic(Bot bot) {
		Trade trade = tradeRepository.findByStatusAndBot(TradeStatus.PENDING, bot);
		if (trade == null) {
			trade = createNewTrade(bot);
		}

		List<Order> orders = orderService.getByBotAndStatus(bot, OrderStatus.PENDING);//TODO search by bot, trade and status
		if (orders.isEmpty()) {
			Strategy strategy = profitAndStrategyService.longTermMarketAnalyzeForStrategy(360);
			orderService.createOrder(bot, strategy);
			return;
		}

		Order order = orders.get(0);
		if (binanceApiService.isOrderFilled(order.getId(), order.getSymbol())) {
			if(trade.getStatus() == TradeStatus.PENDING) {
				createSecondOrderOnTrade(order, trade, bot);
			} else if(trade.getStatus() == TradeStatus.SECOND_ORDER) {
				completeTrade(order, trade, bot);
			}
			return;
		}

		if(trade.getStatus().equals(TradeStatus.PENDING)) {
			double currentPrice = binanceApiService.getCurrentPrice(bot.getMarketPair());
			if (currentPrice <= trade.getBuyPrice() || shouldAdjustBuyOrder(bot, order)) {
				if (binanceApiService.cancelOrder(order.getId(), order.getSymbol())) {
					order.setStatus(OrderStatus.CANCELLED);
					orderService.save(order);
					Strategy strategy = profitAndStrategyService.longTermMarketAnalyzeForStrategy(360);
					Order newOrder = orderService.createOrder(bot, strategy);
					trade.setBuyPrice(newOrder.getPrice());
					tradeRepository.save(trade);
				}
			}
		}
	}

	public Trade createNewTrade(Bot bot) {
		Trade trade = new Trade();
		trade.setBot(bot);
		trade.setStatus(TradeStatus.PENDING);
		trade.setCreatedAt(LocalDateTime.now());
		return tradeRepository.save(trade);
	}

	private void createSecondOrderOnTrade(Order order, Trade trade, Bot bot) {
		Order newOrder = orderService.createOrder(bot, OrderType.opposite(order.getType()));

		order.setStatus(OrderStatus.FILLED);
		trade.setStatus(TradeStatus.SECOND_ORDER);
		trade.setSellPrice(newOrder.getPrice());
		tradeRepository.save(trade);
		orderService.save(order);
	}

	private void completeTrade(Order order, Trade trade, Bot bot) {
		order.setStatus(OrderStatus.FILLED);
		trade.setStatus(TradeStatus.COMPLETED);
		TradeHistory tradeHistory = new TradeHistory();
		tradeHistory.setTrade(trade);
		tradeHistory.setBot(bot);
		tradeHistory.setDetails(String.format(DESCRIPTION_TEMPLATE, trade.getBuyPrice(), trade.getSellPrice()));
		tradeHistory.setCreatedAt(LocalDateTime.now());
		tradeHistoryRepository.save(tradeHistory);
		tradeRepository.save(trade);
	}

	private boolean shouldAdjustBuyOrder(Bot bot, Order order) {
		// Логика для подтяжки цены, если еще не выполнен первый ордер
		LocalDateTime adjustmentDeadline = order.getCreatedAt().plusMinutes(bot.getDeadlineMinutes());
		return adjustmentDeadline.isBefore(LocalDateTime.now());
	}
}
