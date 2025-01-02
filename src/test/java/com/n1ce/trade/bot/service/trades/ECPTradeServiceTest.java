package com.n1ce.trade.bot.service.trades;

import com.n1ce.trade.bot.enums.TradeStatus;
import com.n1ce.trade.bot.model.Bot;
import com.n1ce.trade.bot.model.Trade;
import com.n1ce.trade.bot.repositories.TradeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ECPTradeServiceTest {

	@Mock
	private TradeRepository tradeRepository;

	@InjectMocks
	private ECPTradeService ecpTradeService;

	@Test
	void testFindActiveOrCreateNewTrade_ReturnsActivePendingTrade() {
		Bot bot = Bot.builder().id(1L).build();
		Trade activeTrade = Trade.builder().id(1L).bot(bot).status(TradeStatus.PENDING).build();

		when(tradeRepository.findByStatusAndBot(TradeStatus.PENDING, bot)).thenReturn(activeTrade);

		Trade result = ecpTradeService.findActiveOrCreateNewTrade(bot);

		assertEquals(activeTrade, result);
		verify(tradeRepository).findByStatusAndBot(TradeStatus.PENDING, bot);
		verifyNoMoreInteractions(tradeRepository);
	}

	@Test
	void testFindActiveOrCreateNewTrade_ReturnsSecondOrderTradeWhenPendingNotFound() {
		Bot bot = Bot.builder().id(1L).build();
		Trade secondOrderTrade = Trade.builder().id(2L).bot(bot).status(TradeStatus.SECOND_ORDER).build();

		when(tradeRepository.findByStatusAndBot(TradeStatus.PENDING, bot)).thenReturn(null);
		when(tradeRepository.findByStatusAndBot(TradeStatus.SECOND_ORDER, bot)).thenReturn(secondOrderTrade);

		Trade result = ecpTradeService.findActiveOrCreateNewTrade(bot);

		assertEquals(secondOrderTrade, result);
		verify(tradeRepository).findByStatusAndBot(TradeStatus.PENDING, bot);
		verify(tradeRepository).findByStatusAndBot(TradeStatus.SECOND_ORDER, bot);
		verifyNoMoreInteractions(tradeRepository);
	}

	@Test
	void testFindActiveOrCreateNewTrade_CreatesNewTradeWhenNoActiveTradesFound() {
		Bot bot = Bot.builder().id(1L).build();

		when(tradeRepository.findByStatusAndBot(TradeStatus.PENDING, bot)).thenReturn(null);
		when(tradeRepository.findByStatusAndBot(TradeStatus.SECOND_ORDER, bot)).thenReturn(null);

		Trade result = ecpTradeService.findActiveOrCreateNewTrade(bot);

		assertEquals(bot, result.getBot());
		assertEquals(TradeStatus.PENDING, result.getStatus());
		verify(tradeRepository).findByStatusAndBot(TradeStatus.PENDING, bot);
		verify(tradeRepository).findByStatusAndBot(TradeStatus.SECOND_ORDER, bot);
		verifyNoMoreInteractions(tradeRepository);
	}
}