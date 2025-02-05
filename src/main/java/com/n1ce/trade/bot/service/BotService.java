package com.n1ce.trade.bot.service;

import com.n1ce.trade.bot.dto.AbstractDTO;
import com.n1ce.trade.bot.dto.BotDTO;
import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.repositories.BotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class BotService extends AbstractService<Bot> {
	private final ProfitConfigService profitConfigService;
	private final StrategyService strategyService;

	@Autowired
	public BotService(BotRepository repository, ProfitConfigService profitConfigService, StrategyService strategyService) {
		super(repository);
		this.profitConfigService = profitConfigService;
		this.strategyService = strategyService;
	}

	public List<Bot> getAllActiveBots() {
		return ((BotRepository) repository).findAllByIsRunning(true);
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
