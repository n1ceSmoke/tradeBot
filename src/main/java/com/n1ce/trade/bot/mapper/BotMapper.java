package com.n1ce.trade.bot.mapper;


import com.n1ce.trade.bot.dto.BotDTO;
import com.n1ce.trade.bot.model.Bot;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class BotMapper implements MapperInterface<Bot, BotDTO> {

	private final ModelMapper modelMapper;
	public BotMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
	}

	public BotDTO toDto(Bot bot) {
		return modelMapper.map(bot, BotDTO.class);
	}

	public Bot toEntity(BotDTO botDTO) {
		return modelMapper.map(botDTO, Bot.class);
	}
}