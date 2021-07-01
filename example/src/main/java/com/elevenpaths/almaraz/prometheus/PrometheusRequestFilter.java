package com.elevenpaths.almaraz.prometheus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class PrometheusRequestFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        // TODO: revisit, it could be improved by checking if Actuator is used, is request URL match to <base_path>/prometheus,...
        if (exchange.getRequest().getHeaders().getAccept().stream().anyMatch(mediaType -> MediaType.valueOf("application/openmetrics-text").equalsTypeAndSubtype(mediaType))) {
            log.debug("Request Accepting OpenMetrics MediaType");
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate().header(HttpHeaders.ACCEPT, "application/openmetrics-text; version=1.0.0").build();
            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

            return chain.filter(mutatedExchange);
        }
        else
            return chain.filter(exchange); // No filter applied
    }
}
