package com.n1ce.trade.bot.service;

import com.n1ce.trade.bot.enums.OrderStatus;
import com.n1ce.trade.bot.enums.OrderType;
import com.n1ce.trade.bot.enums.TradeStatus;
import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.model.Order;
import com.n1ce.trade.bot.model.Trade;
import com.n1ce.trade.bot.model.TradeHistory;
import com.n1ce.trade.bot.repositories.TradeHistoryRepository;
import com.n1ce.trade.bot.repositories.TradeRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.n1ce.trade.bot.model.TradeHistory.DESCRIPTION_TEMPLATE;


@Log4j2
public abstract class TradeService extends AbstractService<Trade> {
	protected final TradeRepository tradeRepository;
	protected final OrderService orderService;
	protected final BotService botService;
	protected final TradeHistoryRepository tradeHistoryRepository;
	protected final BinanceApiService binanceApiService;

	protected TradeService(JpaRepository<Trade, Long> repository,
						   TradeRepository tradeRepository,
						   OrderService orderService,
						   BotService botService,
						   TradeHistoryRepository tradeHistoryRepository,
						   BinanceApiService binanceApiService) {
		super(repository);
		this.tradeRepository = tradeRepository;
		this.orderService = orderService;
		this.botService = botService;
		this.tradeHistoryRepository = tradeHistoryRepository;
		this.binanceApiService = binanceApiService;
	}

	public abstract void executeTradeLogic(Bot bot);

	protected void createSecondOrderOnTrade(Bot bot, Trade trade, Order order) {
		Order newOrder = orderService.createOrder(bot, OrderType.opposite(order.getType()), true, trade, order);
		if(null != newOrder) {
			order.setStatus(OrderStatus.FILLED);
			trade.setStatus(TradeStatus.SECOND_ORDER);
			if(order.getType().equals(OrderType.BUY)) trade.setBuyPrice(order.getPrice());
			else trade.setSellPrice(order.getPrice());

			tradeRepository.save(trade);
			orderService.save(order);
		}
	}

	protected boolean checkIsOrderFilled(List<Order> orders, Bot bot, Trade trade) {
		for (Order order : orders) {
			if (binanceApiService.isOrderFilled(order.getId(), order.getSymbol())) {
				log.info("Order is filled. Executing order close action...");
				orders.forEach(o -> executeOrderFilledActions(bot, trade, o));
				return true;
			}
		}
		return false;
	}

	protected void completeTrade(Order order, Trade trade, Bot bot) {
		order.setStatus(OrderStatus.FILLED);
		if(order.getType().equals(OrderType.BUY)) trade.setBuyPrice(order.getPrice());
		else trade.setSellPrice(order.getPrice());
		trade.setStatus(TradeStatus.COMPLETED);

		double profit = (trade.getSellPrice() - trade.getBuyPrice()) * order.getQuantity();
		if(bot.getIsReinvest()) {
			double toReinvest = profit / 2;
			bot.setDeposit(bot.getDeposit() + toReinvest);
			botService.save(bot);
		}

		TradeHistory tradeHistory = new TradeHistory();
		tradeHistory.setTrade(trade);
		tradeHistory.setBot(bot);
		tradeHistory.setDetails(String.format(DESCRIPTION_TEMPLATE, trade.getBuyPrice(), trade.getSellPrice(), profit));
		tradeHistory.setCreatedAt(LocalDateTime.now());
		tradeHistory.setLabel(profit > 0 ? 1 : 0);

		tradeHistoryRepository.save(tradeHistory);
		orderService.save(order);
		tradeRepository.save(trade);
		log.info("Trade completed. Profit {} USDT", profit);
	}

	protected boolean shouldAdjustBuyOrder(Bot bot, Order order) {
		LocalDateTime adjustmentDeadline = order.getCreatedAt().plusMinutes(bot.getDeadlineMinutes());
		return adjustmentDeadline.isBefore(LocalDateTime.now());
	}

	protected void executeOrderFilledActions(Bot bot, Trade trade, Order order) {
		if (trade.getStatus().equals(TradeStatus.PENDING)) {
			createSecondOrderOnTrade(bot, trade, order);
		} else if (trade.getStatus().equals(TradeStatus.SECOND_ORDER)) {
			completeTrade(order, trade, bot);
		}
	}

	protected void checkForProfitPullBack(Bot bot, Trade trade, Order order) {
		double currentPrice = binanceApiService.getCurrentPrice(bot.getMarketPair());
		double targetPrice = order.getPrice();

		if (currentPrice >= targetPrice * bot.getTakeProfitCheckValue()) {
			trade.setHighestPrice(currentPrice);
			tradeRepository.save(trade);
		}

		double pullbackPrice = trade.getHighestPrice() * (1 - bot.getPullbackThreshold() / 100);

		if (currentPrice < pullbackPrice) {
			log.info("Trade by pullback price. Closing price: {}, target price: {}", pullbackPrice, order.getPrice());
			binanceApiService.cancelOrder(order.getId(), order.getSymbol());
			order.setStatus(OrderStatus.CANCELLED);
			orderService.save(order);
			orderService.createOrderWithPrice(bot, order.getType(), pullbackPrice, trade);
		}
	}

	protected void closeTradeIfStuck(Bot bot, Trade trade, Order order) {
		long hoursOpen = Duration.between(trade.getCreatedAt(), LocalDateTime.now()).toHours();
		double currentPrice = binanceApiService.getCurrentPrice(bot.getMarketPair());

		if (hoursOpen >= bot.getMaxTradeHours()) {
			log.info("Trade is closing due to timeframe. Closing price: {}, target price: {}", currentPrice, order.getPrice());
			binanceApiService.cancelOrder(order.getId(), order.getSymbol());
			order.setStatus(OrderStatus.CANCELLED);
			orderService.save(order);
			orderService.createOrderWithPrice(bot, order.getType(), currentPrice, trade);
		}
	}
}
