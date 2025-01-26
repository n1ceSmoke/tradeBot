package com.n1ce.trade.bot.controller;

import com.n1ce.trade.bot.dto.ProfitConfigDTO;
import com.n1ce.trade.bot.mapper.ProfitConfigMapper;
import com.n1ce.trade.bot.model.ProfitConfig;
import com.n1ce.trade.bot.service.ProfitConfigService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profit-config")
public class ProfitConfigController extends  AbstractCrudController<ProfitConfig, ProfitConfigDTO> {
	public ProfitConfigController(ProfitConfigService service, ProfitConfigMapper mapper) {
		super(service, mapper);
	}
}
