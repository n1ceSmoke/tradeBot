package com.n1ce.trade.bot.mapper;

import com.n1ce.trade.bot.dto.TradeHistoryDTO;
import com.n1ce.trade.bot.model.TradeHistory;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class TradeHistoryMapper implements MapperInterface<TradeHistory, TradeHistoryDTO> {

	private final ModelMapper modelMapper;

	public TradeHistoryMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
	}

	public TradeHistoryDTO toDto(TradeHistory tradeHistory) {
		return modelMapper.map(tradeHistory, TradeHistoryDTO.class);
	}

	public TradeHistory toEntity(TradeHistoryDTO tradeHistoryDTO) {
		return modelMapper.map(tradeHistoryDTO, TradeHistory.class);
	}
}
