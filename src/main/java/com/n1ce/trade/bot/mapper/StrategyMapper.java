package com.n1ce.trade.bot.mapper;

import com.n1ce.trade.bot.dto.StrategyDTO;
import com.n1ce.trade.bot.model.Strategy;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class StrategyMapper implements MapperInterface<Strategy, StrategyDTO> {
	private final ModelMapper modelMapper;

	public StrategyMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
	}

	@Override
	public StrategyDTO toDto(Strategy object) {
		return modelMapper.map(object, StrategyDTO.class);
	}

	@Override
	public Strategy toEntity(StrategyDTO dto) {
		return modelMapper.map(dto, Strategy.class);
	}
}
