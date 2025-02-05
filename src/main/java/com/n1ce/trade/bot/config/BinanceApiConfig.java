package com.n1ce.trade.bot.config;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BinanceApiConfig {
	@Value("${binance.api.key}")
	private String apiKey;
	@Value("${binance.secret.key}")
	private String secretKey;

	@Value("${binance.features.api.key}")
	private String featuresApiKey;
	@Value("${binance.features.secret.key}")
	private String featuresSecretKey;

	@Bean
	public UMFuturesClientImpl futuresClient() {
		return new UMFuturesClientImpl(featuresApiKey, featuresSecretKey);
	}

	@Bean
	public BinanceApiRestClient binanceApiRestClient() {
		return BinanceApiClientFactory.newInstance(apiKey, secretKey).newRestClient();
	}
}