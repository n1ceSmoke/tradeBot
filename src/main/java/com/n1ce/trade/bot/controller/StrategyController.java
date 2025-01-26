package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.dto.StrategyDTO;
import com.n1ce.trade.bot.mapper.StrategyMapper;
import com.n1ce.trade.bot.model.Strategy;
import com.n1ce.trade.bot.service.StrategyService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/strategy")
public class StrategyController extends AbstractCrudController<Strategy, StrategyDTO> {
	public StrategyController(StrategyService service, StrategyMapper mapper) {
		super(service, mapper);
	}
}
