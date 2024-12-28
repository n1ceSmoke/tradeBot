package com.n1ce.trade.bot.mapper;

import com.n1ce.trade.bot.dto.OrderDTO;
import com.n1ce.trade.bot.model.Order;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper implements MapperInterface<Order, OrderDTO> {
	private final ModelMapper modelMapper;

	public OrderMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
	}

	public OrderDTO toDto(Order order) {
		return modelMapper.map(order, OrderDTO.class);
	}

	public Order toEntity(OrderDTO orderDTO) {
		return modelMapper.map(orderDTO, Order.class);
	}
}
