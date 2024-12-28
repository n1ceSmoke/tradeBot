package com.n1ce.trade.bot.mapper;

import com.n1ce.trade.bot.dto.AbstractDTO;

public interface MapperInterface<T, C extends AbstractDTO> {
	C toDto(T object);
	T toEntity(C dto);
}
