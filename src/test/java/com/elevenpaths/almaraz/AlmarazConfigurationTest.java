/*
 * Copyright (c) Telefonica I+D. All rights reserved.
 */

package com.elevenpaths.almaraz;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

import com.elevenpaths.almaraz.resolvers.ValidRequestBodyResolver;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link AlmarazConfiguration}.
 *
 * @author Jorge Lorenzo <jorge.lorenzogallardo@telefonica.com>
 *
 */
public class AlmarazConfigurationTest {

	@Test
	public void beans() {
		AlmarazConfiguration config = new AlmarazConfiguration("/api");
		Assert.assertNotNull(config.getJsonSchemaValidator());
		Assert.assertNotNull(config.getContextWebFilter());
		Assert.assertNotNull(config.getLoggerWebFilter());
		Assert.assertNotNull(config.getErrorWebFilter());
		Assert.assertNotNull(config.getCompleteLocationHeaderWebFilter());
		Assert.assertNotNull(config.getBasePathWebFilter());
	}

	@Test
	public void versionBean() {
		AlmarazConfiguration config = new AlmarazConfiguration("/api");
		Assert.assertNull(config.getVersionWebFilter());

		ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
		BuildProperties buildProperties = Mockito.mock(BuildProperties.class);
		config = new AlmarazConfiguration("/api", objectMapper, buildProperties);
		Assert.assertNotNull(config.getVersionWebFilter());
	}

	@Test
	public void configureArgumentResolvers() {
		ArgumentResolverConfigurer configurer = Mockito.mock(ArgumentResolverConfigurer.class);
		AlmarazConfiguration config = new AlmarazConfiguration("/api");
		config.configureArgumentResolvers(configurer);
		Mockito.verify(configurer).addCustomResolver(Mockito.any(ValidRequestBodyResolver.class));
	}

}
