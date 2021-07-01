/*
 * Copyright (c) Telefonica I+D. All rights reserved.
 */

package com.elevenpaths.almaraz.example;

import com.elevenpaths.almaraz.prometheus.PrometheusRequestFilter;
import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.elevenpaths.almaraz.AlmarazConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;

/**
 * Spring {@link Configuration} extending {@link AlmarazConfiguration} to include the default middlewares
 * in the server.
 *
 * @author Jorge Lorenzo <jorge.lorenzogallardo@telefonica.com>
 *
 */
@Configuration
public class ApplicationConfiguration extends AlmarazConfiguration {

	@Bean
	CountedAspect countedAspect(MeterRegistry registry) {
		return new CountedAspect(registry);
	}

	@Bean
	@Order(4)
	public PrometheusRequestFilter getPrometheusRequestFilter() {
		return new PrometheusRequestFilter();
	}

	public ApplicationConfiguration(
			@Value("${almaraz-example.base-path}") String basePath,
			ObjectMapper objectMapper,
			BuildProperties buildProperties) {
		super(basePath, objectMapper, buildProperties);
	}

}
