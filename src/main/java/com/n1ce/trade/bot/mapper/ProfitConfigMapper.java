package com.n1ce.trade.bot.mapper;

import com.n1ce.trade.bot.dto.ProfitConfigDTO;
import com.n1ce.trade.bot.model.ProfitConfig;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ProfitConfigMapper implements MapperInterface<ProfitConfig, ProfitConfigDTO> {

	private final ModelMapper modelMapper;
	public ProfitConfigMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
	}

	@Override
	public ProfitConfigDTO toDto(ProfitConfig object) {
		return modelMapper.map(object, ProfitConfigDTO.class);
	}

	@Override
	public ProfitConfig toEntity(ProfitConfigDTO dto) {
		return modelMapper.map(dto, ProfitConfig.class);
	}
}
