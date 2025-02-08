package com.n1ce.trade.bot.service;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.n1ce.trade.bot.dto.OrderDTO;
import com.n1ce.trade.bot.enums.OrderStatus;
import com.n1ce.trade.bot.enums.OrderType;
import com.n1ce.trade.bot.enums.StrategyType;
import com.n1ce.trade.bot.enums.TradeStatus;
import com.n1ce.trade.bot.mapper.OrderMapper;
import com.n1ce.trade.bot.model.*;
import com.n1ce.trade.bot.repositories.OrderRepository;
import com.n1ce.trade.bot.repositories.TradeRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
public class OrderService extends AbstractService<Order>{

	private final BinanceApiService binanceApiService;
	private final ProfitAndStrategyService profitAndStrategyService;
	private final OrderRepository repository;
	private final OrderMapper orderMapper;

	public OrderService(BinanceApiService binanceApiService,
						ProfitAndStrategyService profitAndStrategyService,
						OrderMapper orderMapper,
						OrderRepository repository) {
		super(repository);
		this.repository = repository;
		this.binanceApiService = binanceApiService;
		this.profitAndStrategyService = profitAndStrategyService;
		this.orderMapper = orderMapper;
	}

	public List<OrderDTO> getByBotID(Long id) {
		List<Order> orders = repository.findByBotIdAndCreatedAtAfter(id, LocalDateTime.now().minusHours(24));
		return orders.stream().sorted(Comparator.comparing(Order::getCreatedAt)).map(orderMapper::toDto).collect(Collectors.toList());
	}

	public List<Order> getByBotAndStatus(Bot bot, OrderStatus status) {
		return repository.findByBotIdAndStatus(bot.getId(), status);
	}

	public void placeOrderIsSignalAboveThan(double signalScore, Bot bot, Trade trade, Boolean isSecondOrder) {
		if (trade.getTradingVector() == null) {
			log.info("No trading vector found for bot " + bot.getId() + ". Skipping order creation.");
			return;
		}

		Signal shortTermSignal = profitAndStrategyService.analyzeMarket(bot);
		if(null != shortTermSignal && shortTermSignal.getStrength() < signalScore) {
			createOrder(bot, trade.getTradingVector(), isSecondOrder, trade);
		}
	}

	public void checkForMarketSignalForOrder(Bot bot, Trade trade) {
		if (trade.getTradingVector() == null) {
			log.info("No trading vector found for bot " + bot.getId() + ". Skipping order creation.");
			return;
		}

		Signal shortTermSignal = profitAndStrategyService.analyzeMarket(bot);
		if(null != shortTermSignal) {
			if (shortTermSignal.getType() == trade.getTradingVector()) {
				createOrder(bot, shortTermSignal.getType(), false, trade);
			} else {
				log.info("Short-term signal disagrees with long-term strategy. Skipping order creation.");
			}
		}
	}

	public void checkForMarketSignalForMarketOrder(Bot bot, Trade trade) {
		if (trade.getTradingVector() == null) {
			log.info("No trading vector found for bot " + bot.getId() + ". Skipping order creation.");
			return;
		}

		Signal shortTermSignal = profitAndStrategyService.analyzeMarket(bot);
		if(null != shortTermSignal && shortTermSignal.getStrength() > 0.6) {
			if (shortTermSignal.getType() == trade.getTradingVector()) {
				createMarketOrder(bot, trade);
			} else {
				log.info("Short-term signal disagrees with long-term strategy. Skipping order creation.");
			}
		} else {
			log.info("Analyze Market signal is low or empty. Skipping order creation.");
		}
	}

	public void createOrder(Bot bot, StrategyType strategy, Boolean isSecondOrder, Trade trade) {
		if(strategy.equals(StrategyType.LONG)) {
			createOrder(bot, OrderType.BUY, isSecondOrder, trade, null);
		} else {
			createOrder(bot, OrderType.SELL, isSecondOrder, trade, null);
		}
	}

	public void createMarketOrder(Bot bot, Trade trade) {
		binanceApiService.setLeverage(bot.getMarketPair(), bot.getLeverage());
		double currentPrice = binanceApiService.getCurrentPrice(bot.getMarketPair());
		double quantity = binanceApiService.adjustFuturesOrderQuantity((bot.getDeposit() * bot.getLeverage()) / currentPrice, bot.getMarketPair(), currentPrice);
		double stopLoss = new BigDecimal(currentPrice * (1 - bot.getFuturesStopLoss() / 100)).setScale(2, RoundingMode.DOWN).doubleValue();
		double takeProfit = new BigDecimal(currentPrice * (1 + bot.getFuturesTakeProfitValue() / 100)).setScale(2, RoundingMode.DOWN).doubleValue();


		Order market = createAndFillOrder(bot, currentPrice, quantity, trade);
		market.setId(binanceApiService.createFuturesOrder(bot.getMarketPair(), trade.getTradingVector().equals(StrategyType.LONG) ? OrderSide.BUY : OrderSide.SELL, quantity));
		save(market);

		Order SLOrder = createAndFillOrder(bot, stopLoss, quantity,trade);
		SLOrder.setId(binanceApiService.createStopLossOrder(bot.getMarketPair(), trade.getTradingVector().equals(StrategyType.LONG) ? OrderSide.BUY : OrderSide.SELL, quantity, stopLoss));
		save(SLOrder);

		Order TPOrder = createAndFillOrder(bot, takeProfit, quantity,trade);
		TPOrder.setId(binanceApiService.createTakeProfitOrder(bot.getMarketPair(), trade.getTradingVector().equals(StrategyType.LONG) ? OrderSide.BUY : OrderSide.SELL, quantity, takeProfit));
		save(TPOrder);

	}

	public Order createOrder(Bot bot, OrderType orderType, Boolean isSecondOrder, Trade trade, Order firstOrder) {
		double profit = isSecondOrder ? profitAndStrategyService.shortTermMarketAnalyzeForProfit(3, bot) : 0.01;
		double currentPrice = firstOrder != null ? firstOrder.getPrice() : binanceApiService.getCurrentPrice(bot.getMarketPair());
		double orderPrice = calculateAmount(orderType, currentPrice, profit);
		double quantity = isSecondOrder ?
				binanceApiService.adjustOrderQuantity(binanceApiService.getAvailableBalance(bot.getMarketPair()), bot.getMarketPair(), orderPrice) :
				binanceApiService.adjustOrderQuantity(bot.getDeposit() / currentPrice, bot.getMarketPair(), orderPrice);
		return createOrderWithQuantity(bot, orderPrice, orderType, trade, quantity);
	}

	public Order createOrderWithPrice(Bot bot, OrderType orderType, double orderPrice, Trade trade) {
		double currentPrice = binanceApiService.getCurrentPrice(bot.getMarketPair());
		return createOrder(bot, currentPrice, orderPrice, orderType, trade);
	}

	public NewOrderResponse createOrder(Bot bot, Order order, OrderType orderType, double orderPrice) {
		return binanceApiService.createOrder(
				bot.getMarketPair(), defineSide(orderType), String.valueOf(order.getQuantity()), scaleToTwo(orderPrice)
		);
	}

	public List<Order> getOrdersByTrade(Trade trade) {
		return repository.findByTradeId(trade.getId());
	}

	private Order createOrder(Bot bot, double orderPrice, double currentPrice, OrderType orderType, Trade trade) {
		Order order = new Order();
		order.setBot(bot);
		order.setPrice(new BigDecimal(orderPrice).setScale(2, RoundingMode.DOWN).doubleValue());
		order.setQuantity(binanceApiService.adjustOrderQuantity(bot.getDeposit() / currentPrice, bot.getMarketPair(), order.getPrice()));
		order.setStatus(OrderStatus.PENDING);
		order.setCreatedAt(LocalDateTime.now());
		order.setType(orderType);
		try {
			NewOrderResponse response = createOrder(bot, order, orderType, orderPrice);
			order.setId(response.getOrderId());
			order.setSymbol(response.getSymbol());
			order.setTrade(trade);

			repository.save(order);
			return order;
		} catch (Exception e) {
			log.info("Error creating order on Binance: " + e.getMessage(), e);
			if (!e.getMessage().contains("Account has insufficient balance for requested action.")) {
				throw new RuntimeException("Error creating order on Binance: " + e.getMessage());
			}
			return null;
		}
	}

	private Order createOrderWithQuantity(Bot bot, double orderPrice, OrderType orderType, Trade trade, double quantity) {
		Order order = new Order();
		order.setBot(bot);
		order.setPrice(new BigDecimal(orderPrice).setScale(2, RoundingMode.DOWN).doubleValue());
		order.setQuantity(quantity);
		order.setStatus(OrderStatus.PENDING);
		order.setCreatedAt(LocalDateTime.now());
		order.setType(orderType);
		try {
			NewOrderResponse response = createOrder(bot, order, orderType, orderPrice);
			order.setId(response.getOrderId());
			order.setSymbol(response.getSymbol());
			order.setTrade(trade);

			repository.save(order);
			return order;
		} catch (Exception e) {
			log.info("Error creating order on Binance: " + e.getMessage(), e);
			if (!e.getMessage().contains("Account has insufficient balance for requested action.")) {
				throw new RuntimeException("Error creating order on Binance: " + e.getMessage());
			}
			return null;
		}
	}

	private Order createAndFillOrder(Bot bot, double price, double quantity, Trade trade) {
		Order order = new Order();
		order.setBot(bot);
		order.setPrice(price);
		order.setQuantity(quantity);
		order.setStatus(OrderStatus.PENDING);
		order.setCreatedAt(LocalDateTime.now());
		order.setTrade(trade);
		order.setSymbol(bot.getMarketPair());
		order.setType(trade.getTradingVector().equals(StrategyType.LONG) ? OrderType.BUY : OrderType.SELL);
		return order;
	}

	private double calculateAmount(OrderType orderType, double currentPrice, double profit) {
		if (orderType.equals(OrderType.BUY)) {
			return currentPrice * (1 - profit / 100);
		} else {
			return currentPrice * (1 + profit / 100);
		}
	}

	private OrderSide defineSide(OrderType orderType) {
		return orderType.equals(OrderType.SELL) ? OrderSide.SELL : OrderSide.BUY;
	}

	private String scaleToTwo(double value) {
		return new BigDecimal(value).setScale(2, RoundingMode.DOWN).toPlainString();
	}
}

