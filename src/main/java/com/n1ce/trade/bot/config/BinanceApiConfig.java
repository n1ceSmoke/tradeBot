package com.n1ce.trade.bot.config;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BinanceApiConfig {
	@Value("${binance.api.key}")
	private String apiKey;
	@Value("${binance.secret.key}")
	private String secretKey;

	@Bean
	public BinanceApiRestClient binanceApiRestClient() {
		return BinanceApiClientFactory.newInstance(apiKey, secretKey).newRestClient();
	}
}