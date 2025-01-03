package com.n1ce.trade.bot.service;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.n1ce.trade.bot.enums.OrderStatus;
import com.n1ce.trade.bot.enums.OrderType;
import com.n1ce.trade.bot.enums.TradeStatus;
import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.model.Order;
import com.n1ce.trade.bot.model.Strategy;
import com.n1ce.trade.bot.repositories.OrderRepository;
import com.n1ce.trade.bot.repositories.TradeRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Log4j2
public class OrderService extends AbstractService<Order>{

	private final BinanceApiService binanceApiService;
	private final TradeRepository tradeRepository;
	private final ProfitAndStrategyService profitAndStrategyService;
	private final OrderRepository repository;

	public OrderService(BinanceApiService binanceApiService,
						TradeRepository tradeRepository,
						ProfitAndStrategyService profitAndStrategyService,
						OrderRepository repository) {
		super(repository);
		this.repository = repository;
		this.binanceApiService = binanceApiService;
		this.tradeRepository = tradeRepository;
		this.profitAndStrategyService = profitAndStrategyService;
	}

	public List<Order> getByBotAndStatus(Bot bot, OrderStatus status) {
		return repository.findByBotIdAndStatus(bot.getId(), status);
	}

	public Order createOrder(Bot bot, Strategy strategy, Boolean isSecondOrder) {
		if(strategy.getName().equals(Strategy.LONG)) {
			return createOrder(bot, OrderType.BUY, isSecondOrder);
		}
		return createOrder(bot, OrderType.SELL, isSecondOrder);
	}

	public Order createOrder(Bot bot, OrderType orderType, Boolean isSecondOrder) {
		double profit = isSecondOrder ? profitAndStrategyService.shortTermMarketAnalyzeForProfit(3, bot) : 0.01;
		double currentPrice = binanceApiService.getCurrentPrice(bot.getMarketPair());
		double orderPrice = calculateAmount(orderType, currentPrice, profit);

		Order order = new Order();
		order.setBot(bot);
		order.setPrice(new BigDecimal(orderPrice).setScale(2, RoundingMode.DOWN).doubleValue());
		order.setQuantity(binanceApiService.adjustOrderQuantity(bot.getDeposit() / currentPrice, bot.getMarketPair(), order.getPrice()));
		order.setStatus(OrderStatus.PENDING);
		order.setCreatedAt(LocalDateTime.now());
		order.setType(orderType);
		try {
			NewOrderResponse response = createOrder(bot, order, orderType, isSecondOrder, currentPrice, orderPrice);
			order.setId(response.getOrderId());
			order.setSymbol(response.getSymbol());
			order.setTrade(tradeRepository.findByStatusAndBot(TradeStatus.PENDING, bot));

			repository.save(order);
			return order;
		} catch (Exception e) {
			log.info("Error creating order on Binance: " + e.getMessage(), e);
			if(!e.getMessage().contains("Account has insufficient balance for requested action.")) {
				throw new RuntimeException("Error creating order on Binance: " + e.getMessage());
			}
			return null;
		}
	}

	private NewOrderResponse createOrder(Bot bot, Order order, OrderType orderType, Boolean isSecondOrder, double currentPrice, double orderPrice) {
//		if(isSecondOrder) {
//			double stopLoss = calculateAmount(orderType, currentPrice, bot.getProfitConfig().getLowProfitThreshold());
//			return binanceApiService.createStopLossOrder(
//					bot.getMarketPair(), defineSide(orderType), String.valueOf(order.getQuantity()), scaleToTwo(orderPrice), scaleToTwo(stopLoss)
//			);
//		} else {
//			return binanceApiService.createOrder(
//					bot.getMarketPair(), defineSide(orderType), String.valueOf(order.getQuantity()), scaleToTwo(orderPrice)
//			);
//		}

		return binanceApiService.createOrder(
				bot.getMarketPair(), defineSide(orderType), String.valueOf(order.getQuantity()), scaleToTwo(orderPrice)
		);
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

