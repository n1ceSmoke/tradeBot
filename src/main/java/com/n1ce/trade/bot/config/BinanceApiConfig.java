package com.n1ce.trade.bot.config;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BinanceApiConfig {

	@Bean
	public BinanceApiRestClient binanceApiRestClient() {
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(
				"YOUR_API_KEY",
				"YOUR_SECRET_KEY"
		);
		return factory.newRestClient();
	}
}