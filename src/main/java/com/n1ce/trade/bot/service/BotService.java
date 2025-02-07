package com.n1ce.trade.bot.service;

import com.n1ce.trade.bot.dto.AbstractDTO;
import com.n1ce.trade.bot.dto.BotDTO;
import com.n1ce.trade.bot.enums.OrderStatus;
import com.n1ce.trade.bot.enums.TradeStatus;
import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.model.Order;
import com.n1ce.trade.bot.model.Trade;
import com.n1ce.trade.bot.repositories.BotRepository;
import com.n1ce.trade.bot.repositories.TradeRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Log4j2
public class BotService extends AbstractService<Bot> {
	private final ProfitConfigService profitConfigService;
	private final StrategyService strategyService;
	private final OrderService orderService;
	private final TradeRepository tradeRepository;
	private final BinanceApiService binanceApiService;

	@Autowired
	public BotService(BinanceApiService binanceApiService, BotRepository repository, ProfitConfigService profitConfigService, StrategyService strategyService, OrderService orderService, TradeRepository tradeRepository) {
		super(repository);
		this.profitConfigService = profitConfigService;
		this.strategyService = strategyService;
		this.orderService = orderService;
		this.tradeRepository = tradeRepository;
		this.binanceApiService = binanceApiService;
	}

	public List<Bot> getAllActiveBots() {
		return ((BotRepository) repository).findAllByIsRunning(true);
	}

	public boolean closeActiveCycleForBot(Long botId) {
		try{
			Bot bot = repository.findById(botId).orElse(null);
			if(bot != null) {
				List<Order> orders = orderService.getByBotAndStatus(bot, OrderStatus.PENDING);
				if(!orders.isEmpty()) {
					orders.forEach(order -> {
						binanceApiService.cancelOrder(order.getId(), bot.getMarketPair());
						order.setStatus(OrderStatus.CANCELLED);
						orderService.save(order);
					});
					Trade trade = orders.get(0).getTrade();
					trade.setStatus(TradeStatus.CANCELED);
					tradeRepository.save(trade);
					return true;
				}
			}
			return true;
		} catch (RuntimeException e) {
			log.info("Cannot cancel cycle for bot {} right now due to {}", botId, e.getMessage());
			return false;
		}
	}

	@Override
	public <D extends AbstractDTO> void update(Long id, D dto) {
		BotDTO botDTO = (BotDTO) dto;
		Bot bot = repository.findById(id).orElse(null);
		if(bot != null) {
			if(Objects.nonNull(botDTO.getDeposit())) {
				bot.setDeposit(botDTO.getDeposit());
			}
			if(Objects.nonNull(botDTO.getName())) {
				bot.setName(botDTO.getName());
			}
			if(Objects.nonNull(botDTO.getDeadlineMinutes())) {
				bot.setDeadlineMinutes(botDTO.getDeadlineMinutes());
			}
			if(Objects.nonNull(botDTO.getMarketPair())) {
				bot.setMarketPair(botDTO.getMarketPair());
			}
			if(Objects.nonNull(botDTO.getRunning())) {
				bot.setIsRunning(botDTO.getRunning());
			}
			if(Objects.nonNull(botDTO.getProfitConfigID())) {
				bot.setProfitConfig(profitConfigService.findById(botDTO.getProfitConfigID()));
			}
			if(Objects.nonNull(botDTO.getStrategyID())) {
				bot.setStrategy(strategyService.findById(botDTO.getStrategyID()));
			}
			if(Objects.nonNull(botDTO.getLeverage())) {
				bot.setLeverage(botDTO.getLeverage());
			}
			if(Objects.nonNull(botDTO.getFuturesStopLoss())) {
				bot.setFuturesStopLoss(botDTO.getFuturesStopLoss());
			}
			if(Objects.nonNull(botDTO.getFuturesTakeProfitValue())) {
				bot.setFuturesTakeProfitValue(botDTO.getFuturesTakeProfitValue());
			}
			repository.save(bot);
		}
	}
}
