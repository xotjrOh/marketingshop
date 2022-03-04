package com.marketingshop.web.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@RequiredArgsConstructor
@Configuration
public class ExternalProperties{

	@Value("${external.mainURL}")
	private String mainURL;

	@Value("${external.apiKey}")
	private String apiKey;

	@Value("${external.id}")
	private String id;

	@Value("${external.password}")
	private String password;
}
