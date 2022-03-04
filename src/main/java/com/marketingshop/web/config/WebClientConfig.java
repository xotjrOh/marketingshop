package com.marketingshop.web.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class WebClientConfig {

    @Autowired
    ExternalProperties externalProperties;

    @Bean
    public WebClient webClient() {

        return WebClient.builder()
                        .baseUrl(externalProperties.getMainURL())
                        .build();
    }

}