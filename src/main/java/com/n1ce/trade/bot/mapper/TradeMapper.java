package com.n1ce.trade.bot.mapper;

import com.n1ce.trade.bot.dto.TradeDTO;
import com.n1ce.trade.bot.model.Trade;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class TradeMapper implements MapperInterface<Trade, TradeDTO> {
	private final ModelMapper modelMapper;

	public TradeMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
	}

	public TradeDTO toDto(Trade trade) {
		return modelMapper.map(trade, TradeDTO.class);
	}

	public Trade toEntity(TradeDTO tradeDTO) {
		return modelMapper.map(tradeDTO, Trade.class);
	}
}
