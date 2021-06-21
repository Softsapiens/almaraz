/*
 * Copyright (c) Telefonica I+D. All rights reserved.
 */

package com.elevenpaths.almaraz.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.LogbackMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.tomcat.TomcatMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(
		exclude = {
				JvmMetricsAutoConfiguration.class,
				LogbackMetricsAutoConfiguration.class,
				SystemMetricsAutoConfiguration.class,
				TomcatMetricsAutoConfiguration.class,
				WebMvcMetricsAutoConfiguration.class
		}
)
public class Application {

	public static void main(String[] args) {



		SpringApplication.run(Application.class, args);
	}

}
